package org.hmmk.sms.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChapaVerifyResponse {
    private String message;
    private String status;
    private ChapaVerifyData data;

    @Data
    @Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChapaVerifyData {
        @JsonProperty("first_name")
        private String firstName;

        @JsonProperty("last_name")
        private String lastName;
        @JsonProperty("phone_number")
        private String phoneNumber;
        private String email;
        private String currency;
        private BigDecimal amount;
        private BigDecimal charge;
        private String mode;
        private String method;
        private String type;
        private String status;
        private String reference;

        @JsonProperty("tx_ref")
        private String txRef;

        @JsonProperty("created_at")
        private String createdAt;

        @JsonProperty("updated_at")
        private String updatedAt;
    }
}
