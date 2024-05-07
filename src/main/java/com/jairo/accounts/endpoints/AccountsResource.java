package com.jairo.accounts.endpoints;

import com.jairo.accounts.domain.Account;
import com.jairo.accounts.endpoints.dto.AccountDTO;
import com.jairo.accounts.repository.AccountsRepository;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import jakarta.inject.Inject;

import java.math.BigDecimal;

public class AccountsResource {
    private static final String PATH_PARAM_INITIAL_BALANCE = "initialBalance";

    private final AccountsRepository accountsRepository;

    @Inject
    public AccountsResource(AccountsRepository accountsRepository) {
        this.accountsRepository = accountsRepository;
    }

    public void createAccount(Context ctx) {
        BigDecimal initialBalance = ctx.pathParamAsClass(PATH_PARAM_INITIAL_BALANCE, BigDecimal.class).get();

        Account account = accountsRepository.createNew(initialBalance);
        ctx.json(new AccountDTO(account.getId()));
        ctx.status(HttpStatus.CREATED);
    }
}
