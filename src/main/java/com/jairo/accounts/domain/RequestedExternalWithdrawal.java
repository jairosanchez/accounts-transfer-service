package com.jairo.accounts.domain;

import com.jairo.accounts.service.WithdrawalService.Address;
import com.jairo.accounts.service.WithdrawalService.WithdrawalId;
import com.jairo.accounts.service.WithdrawalService.WithdrawalState;

import java.math.BigDecimal;

public record RequestedExternalWithdrawal (WithdrawalId withdrawalId, WithdrawalState withdrawalState, BigDecimal amount, Address address) {
}
