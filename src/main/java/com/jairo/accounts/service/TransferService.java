package com.jairo.accounts.service;

import com.jairo.accounts.domain.Account;
import com.jairo.accounts.domain.TransferId;
import com.jairo.accounts.endpoints.dto.ExternalTransferDetails;
import com.jairo.accounts.exception.AccountNotFoundException;
import com.jairo.accounts.exception.TransferIdNotFoundException;
import com.jairo.accounts.repository.AccountsRepository;
import com.jairo.accounts.service.WithdrawalService.Address;
import com.jairo.accounts.service.WithdrawalService.WithdrawalId;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Objects;

import static java.util.UUID.randomUUID;

@Singleton
public class TransferService {

    private final AccountsRepository accountsRepository;
    private final WithdrawalService withdrawalService;
    private final ExternalTransferMonitoringService externalTransferMonitoringService;

    @Inject
    public TransferService(AccountsRepository accountsRepository, WithdrawalService withdrawalService, ExternalTransferMonitoringService externalTransferMonitoringService) {
        this.accountsRepository = accountsRepository;
        this.withdrawalService = withdrawalService;
        this.externalTransferMonitoringService = externalTransferMonitoringService;
    }

    public void transfer(Long senderAccountId, Long receiverAccountId, BigDecimal amount) {
        if (senderAccountId.equals(receiverAccountId)) {
            throw new IllegalArgumentException("Sender and receiver account can't be same");
        }
        Account sender = getAccountOrThrowException(senderAccountId);
        Account receiver = getAccountOrThrowException(receiverAccountId);
        sender.withdraw(amount);
        receiver.deposit(amount);
    }

    public TransferId transfer(Long senderAccountId, Address address, BigDecimal amount) {
        Account sender = getAccountOrThrowException(senderAccountId);
        WithdrawalId withdrawalId = new WithdrawalId(randomUUID());
        sender.requestWithdrawal(amount, withdrawalId, address);
        try {
            withdrawalService.requestWithdrawal(withdrawalId, address, amount);
        } catch (Exception e) {
            //even if withdrawal request hasn't really been sent to external service, we track it as FAILED in the audit.
            sender.failWithdrawal(withdrawalId);
            throw e;
        }
        externalTransferMonitoringService.initiateResponseMonitoring(sender, withdrawalId);
        return new TransferId(withdrawalId.value());
    }

    private Account getAccountOrThrowException(Long accountId) {
        return accountsRepository.findById(accountId).orElseThrow(() -> new AccountNotFoundException("Account with id " + accountId + " not found"));
    }

    public Collection<ExternalTransferDetails> getExternalTransfers(Long accountId) {
        Account account = getAccountOrThrowException(accountId);
        return account.getRequestedExternalWithdrawals().values().stream()
                .map(x -> new ExternalTransferDetails(x.withdrawalId().value(), x.amount(), x.withdrawalState().name(), x.address().value()))
                .toList();
    }

    public ExternalTransferDetails getExternalTransfer(Long accountId, TransferId transferId) {
        return getExternalTransfers(accountId).stream()
                .filter(x -> Objects.equals(x.transferId(), transferId.value()))
                .findFirst().orElseThrow(() -> new TransferIdNotFoundException("Transfer with id " + transferId.value() + " not found"));
    }
}
