package com.jairo.accounts.guice;

import com.google.inject.AbstractModule;
import com.jairo.accounts.endpoints.AccountsResource;
import com.jairo.accounts.endpoints.TransfersResource;
import com.jairo.accounts.service.ExternalTransferMonitoringService;
import com.jairo.accounts.service.TransferService;
import com.jairo.accounts.service.WithdrawalService;
import com.jairo.accounts.service.config.Config;
import com.jairo.accounts.service.stubs.WithdrawalServiceStub;

public class AppModule extends AbstractModule {

    @Override
    protected void configure() {
        //TODO: read params from external config
        bind(Config.class).toInstance(new Config(100, 50));
        bind(WithdrawalService.class).to(WithdrawalServiceStub.class);
        bind(ExternalTransferMonitoringService.class);
        bind(TransferService.class);
        bind(TransfersResource.class);
        bind(AccountsResource.class);
    }
}
