package com.jairo.accounts.service;

import com.jairo.accounts.domain.Account;
import com.jairo.accounts.domain.RequestedExternalWithdrawal;
import com.jairo.accounts.domain.TransferId;
import com.jairo.accounts.exception.NotSufficientFundsException;
import com.jairo.accounts.repository.AccountsRepository;
import com.jairo.accounts.service.WithdrawalService.Address;
import com.jairo.accounts.service.WithdrawalService.WithdrawalId;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static com.jairo.accounts.service.WithdrawalService.WithdrawalState.FAILED;
import static com.jairo.accounts.service.WithdrawalService.WithdrawalState.PROCESSING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private AccountsRepository accountsRepository;
    @Mock
    private WithdrawalService withdrawalService;
    @Mock
    private ExternalTransferMonitoringService externalTransferMonitoringService;

    @InjectMocks
    private TransferService transferService;

    @Test
    void transferSuccessfulBetweenSenderAndReceiverUpdatesBalanceAccordinglyInBoth() {
        Account sender = new Account(1l, BigDecimal.valueOf(1000));

        Account receiver = new Account(2l);

        when(accountsRepository.findById(1l)).thenReturn(Optional.of(sender));
        when(accountsRepository.findById(2l)).thenReturn(Optional.of(receiver));

        transferService.transfer(sender.getId(), receiver.getId(), BigDecimal.valueOf(500));

        assertThat(sender.getBalance()).isEqualTo(BigDecimal.valueOf(500));
        assertThat(receiver.getBalance()).isEqualTo(BigDecimal.valueOf(500));
    }

    @Test
    void transferFromAndToSameAccountThrowsException() {
        Account account = new Account(1l, BigDecimal.valueOf(1000));

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
                transferService.transfer(account.getId(), account.getId(), BigDecimal.valueOf(500)));
    }

    @Test
    void exceptionThrownWhenNotEnoughFundsInSenderAccountAndBalanceRemainsUnchangedInBothSenderAndReceiver() {
        BigDecimal initialBalance = BigDecimal.valueOf(100);
        Account sender = new Account(1l, initialBalance);

        Account receiver = new Account(2l);

        when(accountsRepository.findById(1l)).thenReturn(Optional.of(sender));
        when(accountsRepository.findById(2l)).thenReturn(Optional.of(receiver));

        BigDecimal amountToTransfer = BigDecimal.valueOf(101);
        assertThatExceptionOfType(NotSufficientFundsException.class).isThrownBy(() ->
                transferService.transfer(sender.getId(), receiver.getId(), amountToTransfer));
        assertThat(sender.getBalance()).isEqualTo(initialBalance);
        assertThat(receiver.getBalance()).isZero();
    }

    @Test
    void transferToAddressSuccessfulAndBalanceIsUpdatedInSenderAndExternalRequestRegisteredAsProcessingAndMonitoringInitiated() {
        Account sender = new Account(1l, BigDecimal.valueOf(100));
        BigDecimal amountToWithdraw = BigDecimal.valueOf(95);

        when(accountsRepository.findById(1l)).thenReturn(Optional.of(sender));

        TransferId transferId = transferService.transfer(sender.getId(), new Address("receiver"), amountToWithdraw);

        assertThat(transferId).isNotNull();
        assertThat(transferId.value()).isNotNull();
        assertThat(sender.getRequestedExternalWithdrawals()).hasSize(1);
        assertThat(sender.getRequestedExternalWithdrawals().values()).extracting(RequestedExternalWithdrawal::amount, RequestedExternalWithdrawal::withdrawalState)
                .containsExactlyInAnyOrder(Tuple.tuple(amountToWithdraw, PROCESSING));
        assertThat(sender.getBalance()).isEqualTo(BigDecimal.valueOf(5));
        verify(externalTransferMonitoringService).initiateResponseMonitoring(same(sender), any(WithdrawalId.class));
    }

    @Test
    void transferToAddressFailedAndBalanceRemainsUnChangedInSenderAndExternalRequestRegisteredAsFailedAndMonitoringNotInitiated() {
        Account sender = new Account(1l, BigDecimal.valueOf(100));
        Address receiverAddress = new Address("receiver");
        BigDecimal amountToWithdraw = BigDecimal.valueOf(95);

        when(accountsRepository.findById(1l)).thenReturn(Optional.of(sender));

        doThrow(new RuntimeException("Error connecting to external withdrawal service")).when(withdrawalService)
                .requestWithdrawal(any(WithdrawalId.class), any(Address.class), any(BigDecimal.class));

        transferService.transfer(sender.getId(), receiverAddress, amountToWithdraw);

        assertThat(sender.getRequestedExternalWithdrawals()).hasSize(1);
        assertThat(sender.getRequestedExternalWithdrawals().values()).extracting(RequestedExternalWithdrawal::amount, RequestedExternalWithdrawal::withdrawalState)
                .containsExactlyInAnyOrder(Tuple.tuple(amountToWithdraw, FAILED));
        assertThat(sender.getBalance()).isEqualTo(BigDecimal.valueOf(100));
        verifyNoInteractions(externalTransferMonitoringService);
    }

}