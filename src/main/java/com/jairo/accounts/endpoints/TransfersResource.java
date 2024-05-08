package com.jairo.accounts.endpoints;

import com.jairo.accounts.domain.TransferId;
import com.jairo.accounts.endpoints.dto.ExternalTransferDetails;
import com.jairo.accounts.endpoints.dto.TransferIdDTO;
import com.jairo.accounts.exception.AccountNotFoundException;
import com.jairo.accounts.exception.NotSufficientFundsException;
import com.jairo.accounts.exception.TransferIdNotFoundException;
import com.jairo.accounts.service.TransferService;
import com.jairo.accounts.service.WithdrawalService.Address;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.UnprocessableContentResponse;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;

public class TransfersResource {

    private static final String PATH_PARAM_ACCOUNT_ID = "accountId";
    private static final String PATH_PARAM_SENDER_ACCOUNT_ID = "senderAccountId";
    private static final String PATH_PARAM_RECEIVER_ACCOUNT_ID = "receiverAccountId";

    private static final String PATH_PARAM_TRANSFER_ID = "transferId";

    private static final String PATH_PARAM_ADDRESS = "address";
    private static final String PATH_PARAM_AMOUNT = "amount";

    private final TransferService transferService;

    @Inject
    public TransfersResource(TransferService transferService) {
        this.transferService = transferService;
    }

    public void internalTransfer(Context ctx) {
        run(context -> {
            Long senderAccountId = context.pathParamAsClass(PATH_PARAM_SENDER_ACCOUNT_ID, Long.class).get();
            Long receiverAccountId = context.pathParamAsClass(PATH_PARAM_RECEIVER_ACCOUNT_ID, Long.class).get();
            BigDecimal amount = context.pathParamAsClass(PATH_PARAM_AMOUNT, BigDecimal.class).get();
            transferService.transfer(senderAccountId, receiverAccountId, amount);
        }, ctx);
    }

    public void externalTransfer(Context ctx) {
        run(context -> {
            Long senderAccountId = context.pathParamAsClass(PATH_PARAM_SENDER_ACCOUNT_ID, Long.class).get();
            String address = context.pathParamAsClass(PATH_PARAM_ADDRESS, String.class).get();
            BigDecimal amount = context.pathParamAsClass(PATH_PARAM_AMOUNT, BigDecimal.class).get();
            TransferId transferId = transferService.transfer(senderAccountId, new Address(address), amount);
            context.json(new TransferIdDTO(transferId.value()));
        }, ctx);
    }

    public void listExternalTransfers(Context ctx) {
        run(context -> {
            Long accountId = context.pathParamAsClass(PATH_PARAM_ACCOUNT_ID, Long.class).get();
            Collection<ExternalTransferDetails> data = transferService.getExternalTransfers(accountId);
            context.json(data);
        }, ctx);
    }

    public void getExternalTransfer(Context ctx) {
        run(context -> {
            Long accountId = context.pathParamAsClass(PATH_PARAM_ACCOUNT_ID, Long.class).get();
            UUID transferId = context.pathParamAsClass(PATH_PARAM_TRANSFER_ID, UUID.class).get();
            ExternalTransferDetails data = transferService.getExternalTransfer(accountId, new TransferId(transferId));
            context.json(data);
        }, ctx);

    }

    private void run(Consumer<Context> consumer, Context ctx) {
        try {
            consumer.accept(ctx);
        } catch (AccountNotFoundException | TransferIdNotFoundException e) {
            throw new NotFoundResponse(e.getMessage());
        } catch (NotSufficientFundsException | IllegalArgumentException e) {
            throw new UnprocessableContentResponse(e.getMessage());
        }
    }
}
