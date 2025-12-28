package org.hmmk.sms.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import org.hmmk.sms.dto.ChapaInitResponse;
import org.hmmk.sms.dto.PaymentInitResponse;
import org.hmmk.sms.dto.PaymentTransactionHistoryPoint;
import org.hmmk.sms.dto.common.PaginatedResponse;
import org.hmmk.sms.entity.Tenant;
import org.hmmk.sms.entity.payment.PaymentTransaction;
import org.hmmk.sms.entity.payment.SmsPackageTier;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

        }
    }

    public PaymentTransaction verifyTransaction(PaymentTransaction transaction) {
        // check transaction status
        if (transaction.paymentStatus != PaymentTransaction.PaymentStatus.IN_PROGRESS) {
            return transaction;
        }

        // verify transaction with Chapa
        org.hmmk.sms.dto.ChapaVerifyResponse verifyResponse = chapaPaymentService.verifyPayment(transaction.id);

        // update transaction status based on verification result
        if ("success".equalsIgnoreCase(verifyResponse.getStatus())
                && verifyResponse.getData() != null
                && "success".equalsIgnoreCase(verifyResponse.getData().getStatus())) {

            // Payment successful - update transaction and credit SMS
            transaction.paymentStatus = PaymentTransaction.PaymentStatus.SUCCESSFUL;
            transaction.persist();

            // Credit SMS to tenant
            creditSmsToTenant(transaction.tenantId, transaction.smsCredited);
        }
        return transaction;
    }

    private void creditSmsToTenant(String tenantId, int smsCredits) {
        Tenant tenant = Tenant.findById(tenantId);
        if (tenant != null) {
            tenant.smsCredit += smsCredits;
            tenant.persist();
        }
    }

    /**
     * List payment transactions for a tenant with pagination and optional status
     * filter.
     * 
     * @param tenantId The tenant ID
     * @param page     Page number (0-indexed)
     * @param size     Page size
     * @param status   Optional status filter
     * @return Paginated list of transactions
     */
    public PaginatedResponse<PaymentTransaction> listTransactions(
            String tenantId, int page, int size, PaymentTransaction.PaymentStatus status) {

        io.quarkus.panache.common.Page p = io.quarkus.panache.common.Page.of(page, size);

        String query;
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("tenantId", tenantId);

        if (status != null) {
            query = "tenantId = :tenantId AND paymentStatus = :status ORDER BY createdAt DESC";
            params.put("status", status);
        } else {
            query = "tenantId = :tenantId ORDER BY createdAt DESC";
        }

        List<PaymentTransaction> items = PaymentTransaction.find(query, params).page(p).list();
        long total = PaymentTransaction.count(
                status != null ? "tenantId = :tenantId AND paymentStatus = :status" : "tenantId = :tenantId",
                params);

        return new PaginatedResponse<>(items, total, page, size);
    }

    public PaymentTransaction getTransactionById(String tenantId, String transactionId) {
        String query = "id = :id AND tenantId = :tenantId";
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("id", transactionId);
        params.put("tenantId", tenantId);
        PaymentTransaction transaction = PaymentTransaction.find(query, params).firstResult();
        if (transaction == null) {
            throw new BadRequestException("Transaction not found: " + transactionId);
        }
        return transaction;
    }

    public PaginatedResponse<PaymentTransaction> listAllTransactions(
            int page,
            int size,
            PaymentTransaction.PaymentStatus status,
            String tenantIdFilter) {
        io.quarkus.panache.common.Page p = io.quarkus.panache.common.Page.of(page, size);
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        java.util.List<String> conditions = new java.util.ArrayList<>();

        if (status != null) {
            conditions.add("paymentStatus = :status");
            params.put("status", status);
        }

        if (tenantIdFilter != null && !tenantIdFilter.isBlank()) {
            conditions.add("tenantId = :tenantId");
            params.put("tenantId", tenantIdFilter);
        }

        String orderClause = "ORDER BY createdAt DESC";
        java.util.List<PaymentTransaction> items;
        long total;

        if (conditions.isEmpty()) {
            items = PaymentTransaction.find(orderClause).page(p).list();
            total = PaymentTransaction.count();
        } else {
            String whereClause = String.join(" AND ", conditions);
            items = PaymentTransaction.find(whereClause + " " + orderClause, params).page(p).list();
            total = PaymentTransaction.count(whereClause, params);
        }

        return new PaginatedResponse<>(items, total, page, size);
    }

    public java.util.List<PaymentTransactionHistoryPoint> listTransactionHistory(int days) {
        if (days <= 0) {
            throw new BadRequestException("Days must be greater than zero");
        }

        Instant from = Instant.now().minus(days, ChronoUnit.DAYS);
        Instant startBoundary = from.truncatedTo(ChronoUnit.DAYS);
        java.util.List<PaymentTransaction> transactions = PaymentTransaction
                .find("paymentStatus = ?1 AND createdAt >= ?2 ORDER BY createdAt ASC",
                        PaymentTransaction.PaymentStatus.SUCCESSFUL,
                        startBoundary)
                .list();

        ZoneId zone = ZoneOffset.UTC;
        Map<java.time.LocalDate, java.math.BigDecimal> totalsByDay = new LinkedHashMap<>();

        for (PaymentTransaction tx : transactions) {
            if (tx.createdAt == null || tx.amountPaid == null) {
                continue;
            }
            java.time.LocalDate date = tx.createdAt.atZone(zone).toLocalDate();
            totalsByDay.merge(date, tx.amountPaid, java.math.BigDecimal::add);
        }

        return totalsByDay.entrySet().stream()
                .map(entry -> PaymentTransactionHistoryPoint.builder()
                        .timestamp(entry.getKey().atStartOfDay(zone).toInstant())
                        .totalAmount(entry.getValue())
                        .build())
                .toList();
    }
}
