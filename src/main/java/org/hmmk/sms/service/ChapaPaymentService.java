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

    private static final String CHAPA_API_URL = "https://api.chapa.co/v1/transaction/initialize";

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
                    .returnUrl(returnUrl)
                    .customization(Map.of(
                            "title", "FastSMS Credit Purchase",
                            "description", "SMS Credit Top-up"))
                    .build();

            String requestBody = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(CHAPA_API_URL))
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
}
