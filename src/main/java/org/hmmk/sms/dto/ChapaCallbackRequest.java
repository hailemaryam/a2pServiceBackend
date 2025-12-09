package org.hmmk.sms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ChapaCallbackRequest {
    @JsonProperty("trx_ref")
    private String trxRef;

    @JsonProperty("ref_id")
    private String refId;

    private String status;
}
