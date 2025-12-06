package org.hmmk.sms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ContactDto {

    public String id;

    @NotBlank
    @Pattern(regexp = "^\\+?[0-9A-Za-z-]{4,}$", message = "phone seems invalid")
    public String phone;

    public String name;

    public String email;

}

