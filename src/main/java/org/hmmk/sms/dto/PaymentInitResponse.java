package org.hmmk.sms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInitResponse {
    private String checkoutUrl;
    private String transactionId;
    private int smsCredits;
}
