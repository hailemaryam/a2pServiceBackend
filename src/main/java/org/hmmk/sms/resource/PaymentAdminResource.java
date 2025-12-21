package org.hmmk.sms.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.hmmk.sms.dto.PaymentTransactionHistoryPoint;
import org.hmmk.sms.dto.common.PaginatedResponse;
import org.hmmk.sms.entity.payment.PaymentTransaction;
import org.hmmk.sms.service.PaymentService;

@Path("/api/admin/payments")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Payment Admin", description = "System admin payment oversight")
@RolesAllowed("sys_admin")
public class PaymentAdminResource {

    @Inject
    PaymentService paymentService;

    @GET
    @Path("/transactions")
    @Operation(summary = "List all payment transactions", description = "List all tenant payment transactions with optional filtering")
    public PaginatedResponse<PaymentTransaction> listAllTransactions(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("status") PaymentTransaction.PaymentStatus status,
            @QueryParam("tenantId") String tenantId) {
        return paymentService.listAllTransactions(page, size, status, tenantId);
    }

    @GET
    @Path("/transactions/history")
    @Operation(summary = "Payment transaction history", description = "Aggregate successful payments for graphing")
    public java.util.List<PaymentTransactionHistoryPoint> listTransactionHistory(
            @QueryParam("days") @DefaultValue("30") int days) {
        return paymentService.listTransactionHistory(days);
    }
}
