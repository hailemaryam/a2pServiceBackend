package org.hmmk.sms.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.hmmk.sms.dto.PaymentInitRequest;
import org.hmmk.sms.dto.PaymentInitResponse;
import org.hmmk.sms.dto.common.PaginatedResponse;
import org.hmmk.sms.entity.Tenant;
import org.hmmk.sms.entity.payment.PaymentTransaction;
import org.hmmk.sms.service.PaymentService;

@Path("/api/payments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Payments")
public class PaymentResource {

    @Inject
    JsonWebToken jwt;

    @Inject
    PaymentService paymentService;

    @POST
    @Path("/initialize")
    @RolesAllowed({ "tenant_admin", "tenant_user" })
    @Transactional
    public Response initializePayment(@Valid PaymentInitRequest request) {
        // 1. Get tenant from JWT
        String tenantId = jwt.getClaim("tenantId");
        if (tenantId == null) {
            throw new ForbiddenException("No tenant associated with this user");
        }

        Tenant tenant = Tenant.findById(tenantId);
        if (tenant == null) {
            throw new NotFoundException("Tenant not found");
        }

        // 2. Delegate to service
        PaymentInitResponse response = paymentService.initializePayment(tenant, request.getAmount());

        return Response.ok(response).build();
    }

    /**
     * Callback endpoint for Chapa payment webhook.
     * This endpoint is called by Chapa after payment completion.
     */
    @POST
    @Path("/callback")
    @jakarta.annotation.security.PermitAll
    @Transactional
    public Response handleCallback(org.hmmk.sms.dto.ChapaCallbackRequest callback) {
        paymentService.processCallback(callback.getTrxRef(), callback.getStatus());
        return Response.ok().build();
    }

    /**
     * List payment transactions for the current tenant.
     * Supports pagination and optional status filter.
     */
    @GET
    @Path("/transactions")
    @RolesAllowed({ "tenant_admin", "tenant_user" })
    public PaginatedResponse<PaymentTransaction> listTransactions(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("status") org.hmmk.sms.entity.payment.PaymentTransaction.PaymentStatus status) {

        String tenantId = jwt.getClaim("tenantId");
        if (tenantId == null) {
            throw new ForbiddenException("No tenant associated with this user");
        }

        return paymentService.listTransactions(tenantId, page, size, status);
    }
}
