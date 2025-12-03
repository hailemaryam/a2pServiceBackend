package org.hmmk.sms.dto;

import lombok.Data;

@Data
public class RegistrationRequest {
    private String tenantName;
    private String domain;
    private String username;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
}