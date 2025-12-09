package org.hmmk.sms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ChapaInitResponse {
    private String message;
    private String status;
    private ChapaData data;

    @Data
    public static class ChapaData {
        @JsonProperty("checkout_url")
        private String checkoutUrl;
    }
}
