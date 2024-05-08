package com.jairo.accounts.endpoints;

import com.jairo.accounts.endpoints.dto.AccountDTO;
import com.jairo.accounts.endpoints.dto.ExternalTransferDetails;
import com.jairo.accounts.endpoints.dto.TransferIdDTO;
import com.jairo.accounts.javalin.JavalinApp;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import io.javalin.testtools.HttpClient;
import okhttp3.Response;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;

import static io.javalin.testtools.JavalinTest.test;
import static org.assertj.core.api.Assertions.assertThat;

class EndToEndTests {
    private final Javalin app = new JavalinApp().getApp();
    private final JavalinJackson javalinJackson = new JavalinJackson();

    private Long successfullyCreatedAccount(HttpClient client, BigDecimal initialBalance) throws IOException {
        Response accountResponse = client.post("/accounts/%s".formatted(initialBalance.toPlainString()));
        assertThat(accountResponse.code()).isEqualTo(HttpStatus.CREATED_201);
        AccountDTO newAccount = javalinJackson.fromJsonString(accountResponse.body().string(), AccountDTO.class);
        assertThat(newAccount).isNotNull();
        return newAccount.accountId();
    }

    private static Response internalTransfer(HttpClient client, Long senderAccountId, Long receiverAccountId, float amount) {
        return client.post("/accounts/transfer/internal/from/%s/to/%s/%s".formatted(senderAccountId, receiverAccountId, amount));
    }

    private static Response externalTransfer(HttpClient client, Long senderAccountId, String address, float amount) {
        return client.post("/accounts/transfer/external/from/%s/to/%s/%s".formatted(senderAccountId, address, amount));
    }

    private static Response externalTransferState(HttpClient client, Long senderAccountId, UUID transferId) {
        return client.get("/accounts/%s/transfer/external/%s".formatted(senderAccountId, transferId));
    }

    @Test
    void scenarioSuccessfulInternalTransferBetweenAccounts() {
        test(app, (server, client) -> {
            Long senderAccountId = successfullyCreatedAccount(client, BigDecimal.valueOf(15000.56));
            Long receiverAccountId = successfullyCreatedAccount(client, BigDecimal.ZERO);
            Response transferResponse = internalTransfer(client, senderAccountId, receiverAccountId, 10000);
            assertThat(transferResponse.code()).isEqualTo(HttpStatus.OK_200);
        });
    }

    @Test
    void scenarioFailedInternalTransferFromNonExistentAccount() {
        test(app, (server, client) -> {
            Long receiverAccountId = successfullyCreatedAccount(client, BigDecimal.ZERO);
            Response transferResponse = internalTransfer(client, 999l, receiverAccountId, 10000);
            assertThat(transferResponse.code()).isEqualTo(HttpStatus.NOT_FOUND_404);
        });
    }

    @Test
    void scenarioFailedExternalTransferFromNonExistentAccount() {
        test(app, (server, client) -> {
            Response transferResponse = externalTransfer(client, 999L, "address", 10000);
            assertThat(transferResponse.code()).isEqualTo(HttpStatus.NOT_FOUND_404);
        });
    }

    @Test
    void scenarioFailedInternalTransferToNonExistentAccount() {
        test(app, (server, client) -> {
            Long senderAccountId = successfullyCreatedAccount(client, BigDecimal.valueOf(15000.56));
            Response transferResponse = internalTransfer(client, senderAccountId, 999l, 10000);
            assertThat(transferResponse.code()).isEqualTo(HttpStatus.NOT_FOUND_404);
        });
    }

    @Test
    void scenarioFailedInternalTransferWhenNotEnoughFoundsInSender() {
        test(app, (server, client) -> {
            Long senderAccountId = successfullyCreatedAccount(client, BigDecimal.valueOf(15000.56));
            Long receiverAccountId = successfullyCreatedAccount(client, BigDecimal.ZERO);
            Response transferResponse = internalTransfer(client, senderAccountId, receiverAccountId, 15000.565f);
            assertThat(transferResponse.code()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY_422);
        });
    }

    @Test
    void scenarioFailedExternalTransferWhenNotEnoughFoundsInSender() {
        test(app, (server, client) -> {
            Long senderAccountId = successfullyCreatedAccount(client, BigDecimal.valueOf(15000.56));
            Response transferResponse = externalTransfer(client, senderAccountId, "address", 15000.565f);
            assertThat(transferResponse.code()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY_422);
        });
    }

    @Test
    void scenarioExternalTransferAndGetState() {
        test(app, (server, client) -> {
            Long senderAccountId = successfullyCreatedAccount(client, BigDecimal.valueOf(15000.56));

            Response transfer1CreationResponse = externalTransfer(client, senderAccountId, "address-1", 10000);
            assertThat(transfer1CreationResponse.code()).isEqualTo(HttpStatus.OK_200);
            TransferIdDTO transfer1Id = javalinJackson.fromJsonString(transfer1CreationResponse.body().string(), TransferIdDTO.class);

            Response transfer1StateResponse = externalTransferState(client, senderAccountId, transfer1Id.transferId());
            ExternalTransferDetails transfer1Details = javalinJackson.fromJsonString(transfer1StateResponse.body().string(), ExternalTransferDetails.class);
            assertThat(transfer1Details.status()).isIn("COMPLETED", "FAILED", "PROCESSING");

            Response transfer2CreationResponse = externalTransfer(client, senderAccountId, "address-2", 2000);
            assertThat(transfer2CreationResponse.code()).isEqualTo(HttpStatus.OK_200);
            TransferIdDTO transfer2Id = javalinJackson.fromJsonString(transfer2CreationResponse.body().string(), TransferIdDTO.class);

            Response transfer2StateResponse = externalTransferState(client, senderAccountId, transfer2Id.transferId());
            ExternalTransferDetails transfer2Details = javalinJackson.fromJsonString(transfer2StateResponse.body().string(), ExternalTransferDetails.class);
            assertThat(transfer2Details.status()).isIn("COMPLETED", "FAILED", "PROCESSING");

            Response listOfTransfersResponse = client.get("/accounts/%s/transfers/external".formatted(senderAccountId));
            ExternalTransferDetails[] transferDetails = javalinJackson.fromJsonString(listOfTransfersResponse.body().string(), ExternalTransferDetails[].class);
            assertThat(transferDetails).containsExactlyInAnyOrder(transfer1Details, transfer2Details);
        });
    }
}