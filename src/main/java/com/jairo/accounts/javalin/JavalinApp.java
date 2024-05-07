package com.jairo.accounts.javalin;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.jairo.accounts.endpoints.AccountsResource;
import com.jairo.accounts.endpoints.TransfersResource;
import com.jairo.accounts.guice.AppModule;
import io.javalin.Javalin;

import java.math.BigDecimal;

public class JavalinApp {

    public static final String ACCOUNTS = "/accounts";

    public static final String CREATE_ACCOUNT = ACCOUNTS + "/{initialBalance}";
    private static final String INTERNAL_TRANSFER_PATH = ACCOUNTS + "/transfer/internal/from/{senderAccountId}/to/{receiverAccountId}/{amount}";
    private static final String EXTERNAL_TRANSFER_PATH = ACCOUNTS + "/transfer/external/from/{senderAccountId}/to/{address}/{amount}";
    private static final String EXTERNAL_TRANSFER_LIST = ACCOUNTS + "/{accountId}/transfers/external";

    private final Javalin app;

    public JavalinApp() {
        Injector injector = Guice.createInjector(new AppModule());

        app = Javalin.create(config -> config.validation.register(BigDecimal.class, v -> new BigDecimal(v)))
                .post(CREATE_ACCOUNT, ctx -> injector.getInstance(AccountsResource.class).createAccount(ctx))
                .post(INTERNAL_TRANSFER_PATH, ctx -> injector.getInstance(TransfersResource.class).internalTransfer(ctx))
                .post(EXTERNAL_TRANSFER_PATH, ctx -> injector.getInstance(TransfersResource.class).externalTransfer(ctx))
                .get(EXTERNAL_TRANSFER_LIST, ctx -> injector.getInstance(TransfersResource.class).listExternalTransfers(ctx));

    }

    public Javalin getApp() {
        return app;
    }
}
