package com.jairo.accounts.repository;

import com.jairo.accounts.domain.Account;
import jakarta.inject.Singleton;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Singleton
public class AccountsRepository {

    private AtomicLong sequence = new AtomicLong(1);
    private Map<Long, Account> accountRepo = new ConcurrentHashMap<>();

    public Optional<Account> findById(Long accountId) {
        return Optional.ofNullable(accountRepo.getOrDefault(accountId, null));
    }

    public Account createNew(BigDecimal initialBalance) {
        Account account = new Account(sequence.getAndIncrement(), initialBalance);
        accountRepo.put(account.getId(), account);
        return account;
    }
}
