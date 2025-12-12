package org.hmmk.sms.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class TenantThresholdUpdateRequest {
    @Min(value = 0, message = "Threshold must be non-negative")
    private int approvalThreshold;
}
