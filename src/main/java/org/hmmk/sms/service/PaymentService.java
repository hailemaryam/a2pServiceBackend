package org.hmmk.sms.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import org.hmmk.sms.dto.ChapaInitResponse;
import org.hmmk.sms.dto.PaymentInitResponse;
import org.hmmk.sms.entity.Tenant;
import org.hmmk.sms.entity.payment.PaymentTransaction;
import org.hmmk.sms.entity.payment.SmsPackageTier;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@ApplicationScoped
public class PaymentService {

    @Inject
    ChapaPaymentService chapaPaymentService;

    /**
     * Initialize a payment for SMS credits.
     * 
     * @param tenant The tenant making the payment
     * @param amount The payment amount in ETB
     * @return PaymentInitResponse with checkout URL and transaction details
     */
    public PaymentInitResponse initializePayment(Tenant tenant, BigDecimal amount) {
        // 1. Validate amount
        validateAmount(amount);

        // 2. Find the best tier for this amount
        TierSelectionResult tierResult = selectBestTier(amount);

        // 3. Create payment transaction
        PaymentTransaction transaction = createTransaction(tenant.id, tierResult.tier, amount, tierResult.smsCredits);

        // 4. Call Chapa API
        ChapaInitResponse chapaResponse = chapaPaymentService.initializePayment(
                transaction.id,
                amount.toPlainString(),
                tenant.email,
                tenant.phone);

        // 5. Build and return response
        return PaymentInitResponse.builder()
                .checkoutUrl(chapaResponse.getData().getCheckoutUrl())
                .transactionId(transaction.id)
                .smsCredits(tierResult.smsCredits)
                .build();
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be greater than zero");
        }
    }

    private TierSelectionResult selectBestTier(BigDecimal amount) {
        List<SmsPackageTier> activeTiers = SmsPackageTier
                .find("isActive = true ORDER BY pricePerSms ASC")
                .list();

        if (activeTiers.isEmpty()) {
            throw new BadRequestException("No active SMS package tier available");
        }

        SmsPackageTier bestTier = null;
        int maxSmsCredits = 0;

        for (SmsPackageTier tier : activeTiers) {
            int potentialCredits = amount.divide(tier.pricePerSms, 0, RoundingMode.FLOOR).intValue();

            boolean meetsMinimum = potentialCredits >= tier.minSmsCount;
            boolean meetsMaximum = tier.maxSmsCount == null || potentialCredits <= tier.maxSmsCount;

            if (meetsMinimum && meetsMaximum && potentialCredits > maxSmsCredits) {
                bestTier = tier;
                maxSmsCredits = potentialCredits;
            }
        }

        if (bestTier == null || maxSmsCredits < 1) {
            SmsPackageTier lowestTier = activeTiers.get(0);
            BigDecimal minAmount = lowestTier.pricePerSms.multiply(BigDecimal.valueOf(lowestTier.minSmsCount));
            throw new BadRequestException(
                    "Amount too low. Minimum amount: " + minAmount + " ETB for " + lowestTier.minSmsCount + " SMS");
        }

        return new TierSelectionResult(bestTier, maxSmsCredits);
    }

    private PaymentTransaction createTransaction(String tenantId, SmsPackageTier tier, BigDecimal amount,
            int smsCredits) {
        PaymentTransaction transaction = PaymentTransaction.builder()
                .smsPackage(tier)
                .amountPaid(amount)
                .smsCredited(smsCredits)
                .paymentStatus(PaymentTransaction.PaymentStatus.IN_PROGRESS)
                .build();
        transaction.tenantId = tenantId;
        transaction.persist();
        return transaction;
    }

    /**
     * Internal record to hold tier selection result
     */
    private record TierSelectionResult(SmsPackageTier tier, int smsCredits) {
    }

    /**
     * Process payment callback from Chapa.
     * Verifies the payment and updates transaction status and tenant SMS credits.
     * 
     * @param trxRef The transaction reference (our PaymentTransaction ID)
     * @param status The callback status from Chapa
     */
    public void processCallback(String trxRef, String status) {
        // 1. Find the payment transaction
        PaymentTransaction transaction = PaymentTransaction.findById(trxRef);
        if (transaction == null) {
            throw new BadRequestException("Transaction not found: " + trxRef);
        }

        // Skip if already processed
        if (transaction.paymentStatus != PaymentTransaction.PaymentStatus.IN_PROGRESS) {
            return;
        }

        // 2. Verify payment with Chapa
        org.hmmk.sms.dto.ChapaVerifyResponse verifyResponse = chapaPaymentService.verifyPayment(trxRef);

        // 3. Update transaction status based on verification result
        if ("success".equalsIgnoreCase(verifyResponse.getStatus())
                && verifyResponse.getData() != null
                && "success".equalsIgnoreCase(verifyResponse.getData().getStatus())) {

            // Payment successful - update transaction and credit SMS
            transaction.paymentStatus = PaymentTransaction.PaymentStatus.SUCCESSFUL;
            transaction.persist();

            // Credit SMS to tenant
            creditSmsToTenant(transaction.tenantId, transaction.smsCredited);

        } else {
            // Payment failed
            transaction.paymentStatus = PaymentTransaction.PaymentStatus.FAILED;
            transaction.persist();
        }
    }

    private void creditSmsToTenant(String tenantId, int smsCredits) {
        Tenant tenant = Tenant.findById(tenantId);
        if (tenant != null) {
            tenant.smsCredit += smsCredits;
            tenant.persist();
        }
    }
}
