package com.jairo.accounts.domain;

import com.jairo.accounts.exception.NotSufficientFundsException;
import com.jairo.accounts.service.WithdrawalService.Address;
import com.jairo.accounts.service.WithdrawalService.WithdrawalId;
import com.jairo.accounts.service.WithdrawalService.WithdrawalState;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.unmodifiableMap;

public class Account {

    private final Long id;

    private BigDecimal balance;

    private final Map<WithdrawalId, RequestedExternalWithdrawal> requestedExternalWithdrawals = new ConcurrentHashMap<>();

    public Account(Long id, BigDecimal initialBalance) {
        this.id = id;
        this.balance = initialBalance;
    }

    public Account(Long id) {
        this.id = id;
        this.balance = BigDecimal.ZERO;
    }

    public Long getId() {
        return id;
    }

    public synchronized void deposit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Can't deposit amount <= 0");
        }
        balance = balance.add(amount);
    }

    public synchronized void withdraw(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Can't withdraw amount <= 0");
        }
        if (balance.compareTo(amount) >= 0) {
            balance = balance.subtract(amount);
        } else {
            throw new NotSufficientFundsException("Not sufficient funds available in account %s: current balance is %s".formatted(this.id, balance.toPlainString()));
        }
    }

    public synchronized void requestWithdrawal(BigDecimal amount, WithdrawalId withdrawalId, Address address) {
        withdraw(amount);
        requestedExternalWithdrawals.put(withdrawalId, new RequestedExternalWithdrawal(withdrawalId, WithdrawalState.PROCESSING, amount, address));
    }

    public synchronized void failWithdrawal(WithdrawalId withdrawalId) {
        RequestedExternalWithdrawal requestedExternalWithdrawal = updateWithdrawalRequestState(withdrawalId, WithdrawalState.FAILED);
        balance = balance.add(requestedExternalWithdrawal.amount());
    }

    public void completeWithdrawal(WithdrawalId withdrawalId) {
        updateWithdrawalRequestState(withdrawalId, WithdrawalState.COMPLETED);
    }

    private RequestedExternalWithdrawal updateWithdrawalRequestState(WithdrawalId withdrawalId, WithdrawalState withdrawalState) {
        RequestedExternalWithdrawal requestedExternalWithdrawal = requestedExternalWithdrawals.get(withdrawalId);
        if (requestedExternalWithdrawal == null) {
            throw new IllegalArgumentException("No withdrawal request found for id " + withdrawalId);
        } else {
            requestedExternalWithdrawals.computeIfPresent(withdrawalId, (key, existingValue) ->
                    new RequestedExternalWithdrawal(existingValue.withdrawalId(), withdrawalState, existingValue.amount(), existingValue.address())
            );
            return requestedExternalWithdrawal;
        }
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Map<WithdrawalId, RequestedExternalWithdrawal> getRequestedExternalWithdrawals() {
        return unmodifiableMap(requestedExternalWithdrawals);
    }
}
