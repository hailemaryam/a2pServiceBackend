package org.hmmk.sms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class ChapaInitResponse {
    private String message;
    private String status;
    private ChapaData data;

    @Data
    @Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ChapaData {
        @JsonProperty("checkout_url")
        private String checkoutUrl;
    }
}
