package com.jairo.accounts.endpoints;

import com.jairo.accounts.endpoints.dto.ExternalTransferDetails;
import com.jairo.accounts.exception.AccountNotFoundException;
import com.jairo.accounts.exception.NotSufficientFundsException;
import com.jairo.accounts.service.TransferService;
import com.jairo.accounts.service.WithdrawalService.Address;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.UnprocessableContentResponse;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.Collection;

public class TransfersResource {

    private static final String PATH_PARAM_ACCOUNT_ID = "accountId";
    private static final String PATH_PARAM_SENDER_ACCOUNT_ID = "senderAccountId";
    private static final String PATH_PARAM_RECEIVER_ACCOUNT_ID = "receiverAccountId";

    private static final String PATH_PARAM_ADDRESS = "address";
    private static final String PATH_PARAM_AMOUNT = "amount";

    private final TransferService transferService;

    @Inject
    public TransfersResource(TransferService transferService) {
        this.transferService = transferService;
    }

    public void internalTransfer(Context ctx) {
        Long senderAccountId = ctx.pathParamAsClass(PATH_PARAM_SENDER_ACCOUNT_ID, Long.class).get();
        Long receiverAccountId = ctx.pathParamAsClass(PATH_PARAM_RECEIVER_ACCOUNT_ID, Long.class).get();
        BigDecimal amount = ctx.pathParamAsClass(PATH_PARAM_AMOUNT, BigDecimal.class).get();

        try {
            transferService.transfer(senderAccountId, receiverAccountId, amount);
        } catch (AccountNotFoundException e) {
            throw new NotFoundResponse(e.getMessage());
        } catch (NotSufficientFundsException e) {
            throw new UnprocessableContentResponse(e.getMessage());
        }
    }

    public void externalTransfer(Context ctx) {
        Long senderAccountId = ctx.pathParamAsClass(PATH_PARAM_SENDER_ACCOUNT_ID, Long.class).get();
        String address = ctx.pathParamAsClass(PATH_PARAM_ADDRESS, String.class).get();
        BigDecimal amount = ctx.pathParamAsClass(PATH_PARAM_AMOUNT, BigDecimal.class).get();

        try {
            transferService.transfer(senderAccountId, new Address(address), amount);
        } catch (AccountNotFoundException e) {
            throw new NotFoundResponse();
        } catch (NotSufficientFundsException e) {
            throw new UnprocessableContentResponse(e.getMessage());
        }
    }

    public void listExternalTransfers(Context ctx) {
        Long accountId = ctx.pathParamAsClass(PATH_PARAM_ACCOUNT_ID, Long.class).get();

        try {
            Collection<ExternalTransferDetails> data = transferService.getExternalTransfers(accountId);
            ctx.json(data);
            ctx.status(HttpStatus.OK);
        } catch (AccountNotFoundException e) {
            throw new NotFoundResponse(e.getMessage());
        }
    }
}
