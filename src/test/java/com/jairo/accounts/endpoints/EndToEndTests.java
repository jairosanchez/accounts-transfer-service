package com.jairo.accounts.endpoints;

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

    private static String successfullyCreatedAccount(HttpClient client, BigDecimal initialBalance) throws IOException {
        Response account1Response = client.post("/accounts/%s".formatted(initialBalance.toPlainString()));
        assertThat(account1Response.code()).isEqualTo(HttpStatus.CREATED_201);
        return account1Response.body().string();
    }

    @Test
    void scenarioSuccessfulTransferBetweenInternalAccounts() {
        test(app, (server, client) -> {
            String senderAccountId = successfullyCreatedAccount(client, BigDecimal.valueOf(15000.56));
            String receiverAccountId = successfullyCreatedAccount(client, BigDecimal.ZERO);

            Response transferResponse = client.post("/accounts/transfer/internal/from/%s/to/%s/%s".formatted(senderAccountId, receiverAccountId, 10000));
            assertThat(transferResponse.code()).isEqualTo(HttpStatus.OK_200);
        });
    }

    @Test
    void scenarioFailedTransferFromNonExistentAccount() {
        test(app, (server, client) -> {
            String receiverAccountId = successfullyCreatedAccount(client, BigDecimal.ZERO);

            Response transferResponse = client.post("/accounts/transfer/internal/from/%s/to/%s/%s".formatted(999, receiverAccountId, 10000));
            assertThat(transferResponse.code()).isEqualTo(HttpStatus.NOT_FOUND_404);
        });
    }

    @Test
    void scenarioFailedTransferToNonExistentAccount() {
        test(app, (server, client) -> {
            String senderAccountId = successfullyCreatedAccount(client, BigDecimal.valueOf(15000.56));

            Response transferResponse = client.post("/accounts/transfer/internal/from/%s/to/%s/%s".formatted(senderAccountId, 999, 10000));
            assertThat(transferResponse.code()).isEqualTo(HttpStatus.NOT_FOUND_404);
        });
    }

    @Test
    void scenarioFailedTransferBetweenInternalAccountsWhenNotEnoughFoundsInSender() {
        test(app, (server, client) -> {
            String senderAccountId = successfullyCreatedAccount(client, BigDecimal.valueOf(15000.56));
            String receiverAccountId = successfullyCreatedAccount(client, BigDecimal.ZERO);

            Response transferResponse = client.post("/accounts/transfer/internal/from/%s/to/%s/%s".formatted(senderAccountId, receiverAccountId, 15000.565));
            assertThat(transferResponse.code()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY_422);
        });
    }

    @Test
    void scenarioTransferBetweenInternalAndExternalAddressAndListState() {
        test(app, (server, client) -> {
            String senderAccountId = successfullyCreatedAccount(client, BigDecimal.valueOf(15000.56));

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