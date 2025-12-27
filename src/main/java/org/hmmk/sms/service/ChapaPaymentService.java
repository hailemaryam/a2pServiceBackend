package org.hmmk.sms.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hmmk.sms.dto.ChapaInitRequest;
import org.hmmk.sms.dto.ChapaInitResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class ChapaPaymentService {

    @ConfigProperty(name = "chapa.api-url", defaultValue = "https://api.chapa.co")
    String chapaApiUrl;

    @ConfigProperty(name = "chapa.api-key")
    String chapaApiKey;

    @ConfigProperty(name = "chapa.callback-url")
    String callbackUrl;

    @ConfigProperty(name = "chapa.return-url")
    String returnUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChapaInitResponse initializePayment(String txRef, String amount, String email, String phone) {
        try {
            ChapaInitRequest request = ChapaInitRequest.builder()
                    .amount(amount)
                    .currency("ETB")
                    .email(email != null ? email : "customer@fastsms.dev")
                    .firstName("SMS")
                    .lastName("Customer")
                    .phoneNumber(phone != null ? phone : "0900000000")
                    .txRef(txRef)
                    .callbackUrl(callbackUrl)
                    .returnUrl(returnUrl + txRef)
                    .customization(Map.of(
                            "title", "SMS Credit",
                            "description", "SMS Credit Top-up"))
                    .build();

            String requestBody = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(chapaApiUrl + "/v1/transaction/initialize"))
                    .header("Authorization", "Bearer " + chapaApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            ChapaInitResponse chapaResponse = objectMapper.readValue(response.body(), ChapaInitResponse.class);

            if (!"success".equalsIgnoreCase(chapaResponse.getStatus())) {
                throw new WebApplicationException(
                        "Chapa payment initialization failed: " + chapaResponse.getMessage(),
                        Response.Status.BAD_GATEWAY);
            }

            return chapaResponse;

        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new WebApplicationException(
                    "Failed to initialize payment: " + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Verify a payment transaction with Chapa.
     * 
     * @param txRef The transaction reference (our PaymentTransaction ID)
     * @return ChapaVerifyResponse with payment details
     */
    public org.hmmk.sms.dto.ChapaVerifyResponse verifyPayment(String txRef) {
        try {
            String verifyUrl = chapaApiUrl + "/v1/transaction/verify/" + txRef;

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(verifyUrl))
                    .header("Authorization", "Bearer " + chapaApiKey)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            org.hmmk.sms.dto.ChapaVerifyResponse verifyResponse = objectMapper.readValue(
                    response.body(),
                    org.hmmk.sms.dto.ChapaVerifyResponse.class);

            return verifyResponse;

        } catch (Exception e) {
            throw new WebApplicationException(
                    "Failed to verify payment: " + e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
