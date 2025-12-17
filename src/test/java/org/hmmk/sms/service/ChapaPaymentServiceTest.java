package org.hmmk.sms.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.hmmk.sms.dto.ChapaInitResponse;
import org.hmmk.sms.dto.ChapaVerifyResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class ChapaPaymentServiceTest {

    @Inject
    ChapaPaymentService service;

    static WireMockServer wireMockServer;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(8888);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8888);
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @AfterEach
    void reset() {
        wireMockServer.resetAll();
    }

    @Test
    public void testInitializePayment_Success() {
        stubFor(post(urlEqualTo("/v1/transaction/initialize"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                                "{\"status\":\"success\",\"message\":\"Hosted Link\",\"data\":{\"checkout_url\":\"https://checkout.chapa.co/checkout/payment/123\"}}")
                        .withStatus(200)));

        ChapaInitResponse response = service.initializePayment("TX123", "100", "test@example.com", "0911223344");

        assertNotNull(response);
        assertEquals("success", response.getStatus());
        assertNotNull(response.getData());
        assertEquals("https://checkout.chapa.co/checkout/payment/123", response.getData().getCheckoutUrl());

        verify(postRequestedFor(urlEqualTo("/v1/transaction/initialize"))
                .withRequestBody(containing("TX123"))
                .withRequestBody(containing("100"))
                .withHeader("Authorization", containing("Bearer")));
    }

    @Test
    public void testInitializePayment_Failure() {
        stubFor(post(urlEqualTo("/v1/transaction/initialize"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"failed\",\"message\":\"Invalid Key\"}")
                        .withStatus(200)));

        assertThrows(WebApplicationException.class,
                () -> service.initializePayment("TX123", "100", "test@example.com", "0911223344"));
    }

    @Test
    public void testVerifyPayment_Success() {
        stubFor(get(urlEqualTo("/v1/transaction/verify/TX123"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                                "{\"status\":\"success\",\"message\":\"Payment details\",\"data\":{\"status\":\"success\",\"reference\":\"TX123\",\"amount\":100}}")
                        .withStatus(200)));

        ChapaVerifyResponse response = service.verifyPayment("TX123");

        assertNotNull(response);
        assertEquals("success", response.getStatus());
        assertEquals("TX123", response.getData().getReference());
    }

    @Test
    public void testVerifyPayment_ServerError() {
        stubFor(get(urlEqualTo("/v1/transaction/verify/TX123"))
                .willReturn(aResponse()
                        .withStatus(500)));

        assertThrows(WebApplicationException.class, () -> service.verifyPayment("TX123"));
    }
}
