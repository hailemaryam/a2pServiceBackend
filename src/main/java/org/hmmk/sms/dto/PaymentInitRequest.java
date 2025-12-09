package org.hmmk.sms.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentInitRequest {
    private BigDecimal amount;
}
