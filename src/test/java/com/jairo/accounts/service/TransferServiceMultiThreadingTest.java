package com.jairo.accounts.service;

import com.jairo.accounts.domain.Account;
import com.jairo.accounts.domain.RequestedExternalWithdrawal;
import com.jairo.accounts.repository.AccountsRepository;
import com.jairo.accounts.service.WithdrawalService.Address;
import com.jairo.accounts.service.config.Config;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.jairo.accounts.service.WithdrawalService.WithdrawalState.*;
import static java.util.Collections.synchronizedSet;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TransferServiceMultiThreadingTest {

    private static final int NUMBER_OF_THREADS = 10;
    private static final int MONITORING_DELAY_IN_MILLIS = 100;
    private final ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    @Test
    void multipleTransfersBetweenAccountsInParallelResultInAccurateBalancesOnBothSides() throws ExecutionException, InterruptedException, TimeoutException {
        AccountsRepository accountsRepository = mock(AccountsRepository.class);
        TransferService transferService = new TransferService(accountsRepository, mock(WithdrawalService.class), mock(ExternalTransferMonitoringService.class));

        Account sender = new Account(1l, BigDecimal.valueOf(2000));
        Account receiver = new Account(2l);

        when(accountsRepository.findById(1l)).thenReturn(Optional.of(sender));
        when(accountsRepository.findById(2l)).thenReturn(Optional.of(receiver));

        CompletableFuture[] completableFutures = new CompletableFuture[200];
        for (int i = 0; i < 200; i++) {
            CompletableFuture<Void> completableFuture = runAsync(() -> transferService.transfer(sender.getId(), receiver.getId(), BigDecimal.valueOf(10)), executorService);
            completableFutures[i] = completableFuture;
        }

        CompletableFuture.allOf(completableFutures).get(10, TimeUnit.SECONDS);

        assertThat(sender.getBalance()).isEqualTo(BigDecimal.valueOf(0));
        assertThat(receiver.getBalance()).isEqualTo(BigDecimal.valueOf(2000));
    }

    private class WithdrawalServiceWithSimulatedDelay implements WithdrawalService {

        private final Optional<WithdrawalState> fixedExpectedWithdrawalState;
        private final int minDelayInMillis;
        private final int maxDelayInMillis;

        private final Set<WithdrawalId> failedWithdrawalIds = synchronizedSet(new HashSet<>());

        private WithdrawalServiceWithSimulatedDelay(WithdrawalState fixedExpectedWithdrawalState, int minDelayInMillis, int maxDelayInMillis) {
            this.fixedExpectedWithdrawalState = Optional.of(fixedExpectedWithdrawalState);
            this.minDelayInMillis = minDelayInMillis;
            this.maxDelayInMillis = maxDelayInMillis;
        }

        private WithdrawalServiceWithSimulatedDelay(int minDelayInMillis, int maxDelayInMillis) {
            this.fixedExpectedWithdrawalState = Optional.empty();
            this.minDelayInMillis = minDelayInMillis;
            this.maxDelayInMillis = maxDelayInMillis;
        }

        @Override
        public void requestWithdrawal(WithdrawalId id, Address address, BigDecimal amount) {
            try {
                MILLISECONDS.sleep(ThreadLocalRandom.current().nextInt(minDelayInMillis, maxDelayInMillis));
            } catch (InterruptedException e) {
            }
        }

        @Override
        public WithdrawalState getRequestState(WithdrawalId id) {
            try {
                MILLISECONDS.sleep(ThreadLocalRandom.current().nextInt(minDelayInMillis, maxDelayInMillis));
            } catch (InterruptedException e) {
            } finally {
                WithdrawalState withdrawalState = fixedExpectedWithdrawalState.orElseGet(() -> ThreadLocalRandom.current().nextBoolean() ? COMPLETED : FAILED);
                if (withdrawalState == FAILED) {
                    failedWithdrawalIds.add(id);
                }
                return withdrawalState;
            }
        }

    }

    @Test
    void multipleProcessingTransfersToExternalWithdrawalServiceResultInAccurateBalanceOnSender() throws ExecutionException, InterruptedException, TimeoutException {
        WithdrawalServiceWithSimulatedDelay withdrawalService = new WithdrawalServiceWithSimulatedDelay(PROCESSING, 100, 200);
        AccountsRepository accountsRepository = new AccountsRepository();
        Config config = new Config(MONITORING_DELAY_IN_MILLIS, NUMBER_OF_THREADS);
        TransferService transferService = new TransferService(accountsRepository, withdrawalService, new ExternalTransferMonitoringService(withdrawalService, config));

        Account sender = accountsRepository.createNew(BigDecimal.valueOf(10000));

        Set<BigDecimal> amountsToTransfer = Stream.iterate(1, n -> n + 1).limit(100).map(BigDecimal::valueOf).collect(Collectors.toSet());

        Collection<CompletableFuture> completableFutures = new ArrayList<>();
        amountsToTransfer.forEach(amount -> {
            CompletableFuture<Void> completableFuture = runAsync(() -> transferService.transfer(sender.getId(), new Address("address"), amount), executorService);
            completableFutures.add(completableFuture);
        });

        CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0])).get(10, TimeUnit.SECONDS);

        assertThat(sender.getBalance()).isEqualTo(BigDecimal.valueOf(4950));
        assertThat(sender.getRequestedExternalWithdrawals().values()).hasSize(100);
        assertThat(sender.getRequestedExternalWithdrawals().values()).extracting(RequestedExternalWithdrawal::amount)
                .containsExactlyInAnyOrder(amountsToTransfer.toArray(new BigDecimal[0]));
        assertThat(sender.getRequestedExternalWithdrawals().values()).describedAs("All request are in processing state").allMatch(x -> x.withdrawalState() == PROCESSING);
    }

    @Test
    void multipleParallelTransfersBetweenSameTwoAccountsDoNoCauseDeadlock() throws ExecutionException, InterruptedException, TimeoutException {
        AccountsRepository accountsRepository = mock(AccountsRepository.class);
        TransferService transferService = new TransferService(accountsRepository, mock(WithdrawalService.class), mock(ExternalTransferMonitoringService.class));

        Account account1 = new Account(1l, BigDecimal.valueOf(2000));
        Account account2 = new Account(2l, BigDecimal.valueOf(4000));

        when(accountsRepository.findById(1l)).thenReturn(Optional.of(account1));
        when(accountsRepository.findById(2l)).thenReturn(Optional.of(account2));

        Collection<CompletableFuture> completableFutures = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            CompletableFuture<Void> completableFutureAccount1ToAccount2 = runAsync(() -> transferService.transfer(account1.getId(), account2.getId(), BigDecimal.valueOf(10)), executorService);
            completableFutures.add(completableFutureAccount1ToAccount2);
            CompletableFuture<Void> completableFutureAccount2ToAccount1 = runAsync(() -> transferService.transfer(account2.getId(), account1.getId(), BigDecimal.valueOf(10)), executorService);
            completableFutures.add(completableFutureAccount2ToAccount1);
        }

        CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0])).get(10, TimeUnit.SECONDS);

        assertThat(account1.getBalance()).isEqualTo(BigDecimal.valueOf(2000));
        assertThat(account2.getBalance()).isEqualTo(BigDecimal.valueOf(4000));
    }

    @Test
    void multipleCompletedAndFailedTransfersToExternalWithdrawalServiceResultInAccurateBalanceOnSender() throws ExecutionException, InterruptedException, TimeoutException {
        WithdrawalServiceWithSimulatedDelay withdrawalService = new WithdrawalServiceWithSimulatedDelay(100, 200);
        AccountsRepository accountsRepository = new AccountsRepository();
        Config config = new Config(MONITORING_DELAY_IN_MILLIS, NUMBER_OF_THREADS);
        TransferService transferService = new TransferService(accountsRepository, withdrawalService, new ExternalTransferMonitoringService(withdrawalService, config));

        Account sender = accountsRepository.createNew(BigDecimal.valueOf(10000));

        Set<BigDecimal> amountsToTransfer = Stream.iterate(1, n -> n + 1).limit(100).map(BigDecimal::valueOf).collect(Collectors.toSet());

        Collection<CompletableFuture> completableFutures = new ArrayList<>();
        amountsToTransfer.forEach(amount -> {
            CompletableFuture<Void> completableFuture = runAsync(() -> transferService.transfer(sender.getId(), new Address("address"), amount), executorService);
            completableFutures.add(completableFuture);
        });

        CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0])).get(10, TimeUnit.SECONDS);

        await().atMost(10, TimeUnit.SECONDS).until(() -> sender.getRequestedExternalWithdrawals().values().stream().allMatch(this::isFinalState));

        BigDecimal amountOfCompletedTransferRequests = BigDecimal.ZERO;
        for (RequestedExternalWithdrawal r : sender.getRequestedExternalWithdrawals().values()) {
            if (!withdrawalService.failedWithdrawalIds.contains(r.withdrawalId())) {
                assertThat(r.withdrawalState()).isEqualTo(COMPLETED);
                amountOfCompletedTransferRequests = amountOfCompletedTransferRequests.add(r.amount());
            } else {
                assertThat(r.withdrawalState()).isEqualTo(FAILED);
            }
        }

        assertThat(sender.getBalance()).isEqualTo(BigDecimal.valueOf(10000).subtract(amountOfCompletedTransferRequests));
    }

    private boolean isFinalState(RequestedExternalWithdrawal requestedExternalWithdrawal) {
        return requestedExternalWithdrawal.withdrawalState() != PROCESSING;
    }


}
