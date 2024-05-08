package com.jairo.accounts.domain;

import com.jairo.accounts.service.WithdrawalService.Address;
import com.jairo.accounts.service.WithdrawalService.WithdrawalId;
import com.jairo.accounts.service.WithdrawalService.WithdrawalState;

import java.math.BigDecimal;
import java.util.Objects;

public class RequestedExternalWithdrawal {
    private final WithdrawalId withdrawalId;
    private final BigDecimal amount;
    private final Address address;
    private WithdrawalState withdrawalState;


    public RequestedExternalWithdrawal(WithdrawalId withdrawalId, WithdrawalState withdrawalState, BigDecimal amount, Address address) {
        this.withdrawalId = withdrawalId;
        this.amount = amount;
        this.withdrawalState = withdrawalState;
        this.address = address;
    }

    public void setWithdrawalState(WithdrawalState withdrawalState) {
        this.withdrawalState = withdrawalState;
    }

    public WithdrawalId getWithdrawalId() {
        return withdrawalId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public WithdrawalState getWithdrawalState() {
        return withdrawalState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequestedExternalWithdrawal that = (RequestedExternalWithdrawal) o;
        return Objects.equals(withdrawalId, that.withdrawalId) && Objects.equals(amount, that.amount) && Objects.equals(address, that.address) && withdrawalState == that.withdrawalState;
    }

    @Override
    public int hashCode() {
        return Objects.hash(withdrawalId, amount, address, withdrawalState);
    }

    public Address getAddress() {
        return address;
    }
}
