package org.hmmk.sms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TenantCreateRequest {
    @NotBlank(message = "Tenant name is required")
    private String name;

    private String phone;

    private boolean isCompany;

    private String tinNumber;

    private String description;
}
