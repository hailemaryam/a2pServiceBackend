package org.hmmk.sms.dto;

import lombok.Data;
import org.hmmk.sms.entity.Tenant;

import java.time.Instant;

@Data
public class TenantResponse {
    private String id;
    private String name;
    private String email;
    private String phone;
    private Tenant.TenantStatus status;
    private long smsCredit;
    private int smsApprovalThreshold;
    private boolean isCompany;
    private String tinNumber;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;

    public static TenantResponse from(Tenant tenant) {
        TenantResponse response = new TenantResponse();
        response.setId(tenant.id);
        response.setName(tenant.name);
        response.setEmail(tenant.email);
        response.setPhone(tenant.phone);
        response.setStatus(tenant.status);
        response.setSmsCredit(tenant.smsCredit);
        response.setSmsApprovalThreshold(tenant.smsApprovalThreshold);
        response.setCompany(tenant.isCompany);
        response.setTinNumber(tenant.tinNumber);
        response.setDescription(tenant.description);
        response.setCreatedAt(tenant.createdAt);
        response.setUpdatedAt(tenant.updatedAt);
        return response;
    }
}
