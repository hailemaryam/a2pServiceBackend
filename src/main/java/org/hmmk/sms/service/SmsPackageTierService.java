package org.hmmk.sms.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.hmmk.sms.entity.payment.SmsPackageTier;

import java.math.BigDecimal;

@ApplicationScoped
public class SmsPackageTierService {
    public BigDecimal calculatePrice(int requestedSmsCount) {
        SmsPackageTier tier = SmsPackageTier.find(
                "minSmsCount <= ?1 AND (maxSmsCount IS NULL OR maxSmsCount >= ?1) AND isActive = true",
                requestedSmsCount
        ).firstResult();

        if (tier == null) {
            throw new IllegalArgumentException("No pricing tier available for this SMS count");
        }

        return BigDecimal.valueOf(requestedSmsCount).multiply(tier.pricePerSms);
    }

}
