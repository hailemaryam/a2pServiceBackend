package org.hmmk.sms.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hmmk.sms.entity.Tenant;

@Data
public class TenantStatusUpdateRequest {
    @NotNull(message = "Status is required")
    private Tenant.TenantStatus status;
}
