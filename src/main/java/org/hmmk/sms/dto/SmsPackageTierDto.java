package org.hmmk.sms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class SmsPackageTierDto {

    @NotNull
    @Min(0)
    public Integer minSmsCount;

    public Integer maxSmsCount;

    @NotNull
    public BigDecimal pricePerSms;

    public String description;

    @NotNull
    public Boolean isActive;

}

