package com.jairo.accounts.domain;

import com.jairo.accounts.service.WithdrawalService.Address;
import com.jairo.accounts.service.WithdrawalService.WithdrawalId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.UUID;

import static com.jairo.accounts.service.WithdrawalService.WithdrawalState.PROCESSING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class AccountTest {

    @Test
    void depositAmountOnAccount() {
        Account account = new Account(1l);
        account.deposit(BigDecimal.TEN);

        assertThat(account.getBalance()).isEqualTo(BigDecimal.TEN);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    void depositNegativeOrZeroAmountNotAllowed(int amount) {
        Account account = new Account(1l);

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> account.deposit(BigDecimal.valueOf(amount)));
    }

    @Test
    void withdrawAmountFromAccount() {
        Account account = new Account(1l, BigDecimal.TEN);
        account.withdraw(BigDecimal.valueOf(2));

        assertThat(account.getBalance()).isEqualTo(BigDecimal.valueOf(8));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    void withdrawNegativeOrZeroAmountNotAllowed(int amount) {
        Account account = new Account(1l, BigDecimal.TEN);

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> account.withdraw(BigDecimal.valueOf(amount)));
    }

    @Test
    void requestExternalWithdrawalOnAccount() {
        Account account = new Account(1l, BigDecimal.TEN);
        WithdrawalId withdrawalId = new WithdrawalId(UUID.randomUUID());
        Address address = new Address("address");
        account.requestWithdrawal(BigDecimal.TEN, withdrawalId, address);

        assertThat(account.getBalance()).isEqualTo(BigDecimal.ZERO);
        assertThat(account.getRequestedExternalWithdrawals()).containsKey(withdrawalId)
                .containsValue(new RequestedExternalWithdrawal(withdrawalId, PROCESSING, BigDecimal.TEN, address));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    void requestExternalWithdrawalWithNegativeOrZeroAmountNotAllowed(int amount) {
        Account account = new Account(1l, BigDecimal.TEN);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> account.requestWithdrawal(BigDecimal.valueOf(amount), new WithdrawalId(UUID.randomUUID()), new Address("address")));
    }
}