package com.jairo.accounts.service;

import com.jairo.accounts.domain.Account;
import com.jairo.accounts.service.WithdrawalService.WithdrawalState;
import com.jairo.accounts.service.config.Config;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Singleton
public class ExternalTransferMonitoringService {

    private final ScheduledExecutorService executorService;
    private final WithdrawalService withdrawalService;
    private final int monitoringDelayInMillis;

    @Inject
    public ExternalTransferMonitoringService(WithdrawalService withdrawalService, Config config) {
        if (config.getNumberOfMonitoringThreads() <= 0) {
            throw new IllegalArgumentException("Number of monitoring threads must be > 0");
        }
        if (config.getMonitoringDelayInMillis() <= 0) {
            throw new IllegalArgumentException("Monitoring delay in millis must be > 0");
        }
        this.withdrawalService = withdrawalService;
        this.executorService = Executors.newScheduledThreadPool(config.getNumberOfMonitoringThreads());
        this.monitoringDelayInMillis = config.getMonitoringDelayInMillis();
    }

    public void initiateResponseMonitoring(Account sender, WithdrawalService.WithdrawalId withdrawalId) {
        executorService.schedule(() -> {
            WithdrawalState requestState = withdrawalService.getRequestState(withdrawalId);
            switch (requestState) {
                case PROCESSING -> initiateResponseMonitoring(sender, withdrawalId);
                case COMPLETED -> sender.completeWithdrawal(withdrawalId);
                case FAILED -> sender.failWithdrawal(withdrawalId);
            }
        }, monitoringDelayInMillis, TimeUnit.MILLISECONDS);
    }
}
