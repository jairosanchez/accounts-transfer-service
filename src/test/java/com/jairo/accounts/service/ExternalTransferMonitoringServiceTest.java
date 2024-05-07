package com.jairo.accounts.service;

import com.jairo.accounts.domain.Account;
import com.jairo.accounts.service.WithdrawalService.Address;
import com.jairo.accounts.service.WithdrawalService.WithdrawalId;
import com.jairo.accounts.service.WithdrawalService.WithdrawalState;
import com.jairo.accounts.service.config.Config;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static com.jairo.accounts.service.WithdrawalService.WithdrawalState.*;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExternalTransferMonitoringServiceTest {

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    void classConstructorFailsIfIncorrectNumberOfThreadsProvided(int numberOfThreads) {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new ExternalTransferMonitoringService(mock(WithdrawalService.class), new Config( 100, numberOfThreads)));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    void classConstructorFailsIfIncorrectMonitoringDelayProvided(int monitoringDelayInMillis) {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new ExternalTransferMonitoringService(mock(WithdrawalService.class), new Config( monitoringDelayInMillis, 1)));
    }

    @Test
    void classConstructorSucceedsIfCorrectNumberOfThreadsProvided() {
        ExternalTransferMonitoringService externalTransferMonitoringService = new ExternalTransferMonitoringService(mock(WithdrawalService.class), new Config( 100, 1));
        assertThat(externalTransferMonitoringService).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(value = WithdrawalState.class, names = {"FAILED", "COMPLETED"}, mode = EnumSource.Mode.INCLUDE)
    void monitorsTransferRequestUntilCompletedOrFailed(WithdrawalState finalWithdrawalState) {
        WithdrawalService withdrawalService = mock(WithdrawalService.class);
        ExternalTransferMonitoringService externalTransferMonitoringService = new ExternalTransferMonitoringService(withdrawalService, new Config( 100, 1));

        Account sender = new Account(1l);
        WithdrawalId withdrawalId = new WithdrawalId(randomUUID());
        sender.deposit(BigDecimal.valueOf(1000));
        sender.requestWithdrawal(BigDecimal.valueOf(500), withdrawalId, new Address("address"));

        when(withdrawalService.getRequestState(withdrawalId)).thenReturn(PROCESSING, PROCESSING, PROCESSING, finalWithdrawalState);

        externalTransferMonitoringService.initiateResponseMonitoring(sender, withdrawalId);

        await().atMost(2, TimeUnit.SECONDS).until(() -> sender.getRequestedExternalWithdrawals().get(withdrawalId).getWithdrawalState() == finalWithdrawalState);
        if (finalWithdrawalState == COMPLETED) {
            assertThat(sender.getBalance()).isEqualTo(BigDecimal.valueOf(500));
        }
        if (finalWithdrawalState == FAILED) {
            assertThat(sender.getBalance()).isEqualTo(BigDecimal.valueOf(1000));
        }
    }

}