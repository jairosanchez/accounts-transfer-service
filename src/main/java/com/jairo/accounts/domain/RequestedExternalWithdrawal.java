package com.jairo.accounts.domain;

import com.jairo.accounts.service.WithdrawalService.Address;
import com.jairo.accounts.service.WithdrawalService.WithdrawalId;
import com.jairo.accounts.service.WithdrawalService.WithdrawalState;

import java.math.BigDecimal;

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

    public Address getAddress() {
        return address;
    }
}
