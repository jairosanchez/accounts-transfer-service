package com.jairo.accounts.endpoints;

import com.jairo.accounts.endpoints.dto.AccountDTO;
import com.jairo.accounts.endpoints.dto.ExternalTransferDetails;
import com.jairo.accounts.javalin.JavalinApp;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import io.javalin.testtools.HttpClient;
import okhttp3.Response;
import org.assertj.core.groups.Tuple;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;

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

    @Test
    void scenarioSuccessfulTransferBetweenInternalAccounts() {
        test(app, (server, client) -> {
            Long senderAccountId = successfullyCreatedAccount(client, BigDecimal.valueOf(15000.56));
            Long receiverAccountId = successfullyCreatedAccount(client, BigDecimal.ZERO);

            Response transferResponse = client.post("/accounts/transfer/internal/from/%s/to/%s/%s".formatted(senderAccountId, receiverAccountId, 10000));
            assertThat(transferResponse.code()).isEqualTo(HttpStatus.OK_200);
        });
    }

    @Test
    void scenarioFailedTransferFromNonExistentAccount() {
        test(app, (server, client) -> {
            Long receiverAccountId = successfullyCreatedAccount(client, BigDecimal.ZERO);

            Response transferResponse = client.post("/accounts/transfer/internal/from/%s/to/%s/%s".formatted(999, receiverAccountId, 10000));
            assertThat(transferResponse.code()).isEqualTo(HttpStatus.NOT_FOUND_404);
        });
    }

    @Test
    void scenarioFailedTransferToNonExistentAccount() {
        test(app, (server, client) -> {
            Long senderAccountId = successfullyCreatedAccount(client, BigDecimal.valueOf(15000.56));

            Response transferResponse = client.post("/accounts/transfer/internal/from/%s/to/%s/%s".formatted(senderAccountId, 999, 10000));
            assertThat(transferResponse.code()).isEqualTo(HttpStatus.NOT_FOUND_404);
        });
    }

    @Test
    void scenarioFailedTransferBetweenInternalAccountsWhenNotEnoughFoundsInSender() {
        test(app, (server, client) -> {
            Long senderAccountId = successfullyCreatedAccount(client, BigDecimal.valueOf(15000.56));
            Long receiverAccountId = successfullyCreatedAccount(client, BigDecimal.ZERO);

            Response transferResponse = client.post("/accounts/transfer/internal/from/%s/to/%s/%s".formatted(senderAccountId, receiverAccountId, 15000.565));
            assertThat(transferResponse.code()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY_422);
        });
    }

    @Test
    void scenarioTransferBetweenInternalAndExternalAddressAndListState() {
        test(app, (server, client) -> {
            Long senderAccountId = successfullyCreatedAccount(client, BigDecimal.valueOf(15000.56));

            Response transfer1Response = client.post("/accounts/transfer/external/from/%s/to/%s/%s".formatted(senderAccountId, "address-1", 10000));
            assertThat(transfer1Response.code()).isEqualTo(HttpStatus.OK_200);

            Response transfer2Response = client.post("/accounts/transfer/external/from/%s/to/%s/%s".formatted(senderAccountId, "address-2", 2000));
            assertThat(transfer2Response.code()).isEqualTo(HttpStatus.OK_200);

            Response listOfTransfersResponse = client.get("/accounts/%s/transfers/external".formatted(senderAccountId));
            ExternalTransferDetails[] transferDetails = javalinJackson.fromJsonString(listOfTransfersResponse.body().string(), ExternalTransferDetails[].class);
            assertThat(transferDetails).extracting(ExternalTransferDetails::address, ExternalTransferDetails::amount)
                    .containsExactlyInAnyOrder(
                            Tuple.tuple("address-1", BigDecimal.valueOf(10000)),
                            Tuple.tuple("address-2", BigDecimal.valueOf(2000)));
        });
    }
}