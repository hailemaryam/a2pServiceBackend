package org.hmmk.sms.dto;

import lombok.Data;

@Data
public class RegistrationRequest {
    private String tenantName;
    private String domain;
    private String adminUsername;
    private String adminEmail;
    private String adminPassword;
}