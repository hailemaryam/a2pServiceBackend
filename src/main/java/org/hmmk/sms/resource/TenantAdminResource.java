package org.hmmk.sms.resource;

import io.quarkus.panache.common.Page;
import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.hmmk.sms.dto.common.PaginatedResponse;
import org.hmmk.sms.dto.TenantResponse;
import org.hmmk.sms.dto.TenantStatusUpdateRequest;
import org.hmmk.sms.dto.TenantThresholdUpdateRequest;
import org.hmmk.sms.entity.Tenant;

import java.util.List;

@Path("/api/admin/tenants")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Tenant Admin", description = "System Admin Tenant Management")
@RolesAllowed("sys_admin")
public class TenantAdminResource {

    @GET
    @Operation(summary = "List tenants", description = "List all tenants with pagination")
    @APIResponse(responseCode = "200", description = "Paginated list of tenants")
    public PaginatedResponse<TenantResponse> list(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        Page p = Page.of(page, size);
        List<Tenant> tenants = Tenant.findAll().page(p).list();
        long total = Tenant.count();

        List<TenantResponse> items = tenants.stream()
                .map(TenantResponse::from)
                .toList();

        return new PaginatedResponse<>(items, total, page, size);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get tenant", description = "Get tenant by ID")
    @APIResponse(responseCode = "200", description = "Tenant details", content = @Content(schema = @Schema(implementation = TenantResponse.class)))
    @APIResponse(responseCode = "404", description = "Tenant not found")
    public TenantResponse getById(@PathParam("id") String id) {
        Tenant tenant = Tenant.findById(id);
        if (tenant == null) {
            throw new NotFoundException("Tenant not found");
        }
        return TenantResponse.from(tenant);
    }

    @PUT
    @Path("/{id}/status")
    @Transactional
    @Operation(summary = "Update tenant status", description = "Activate or Deactivate a tenant")
    @APIResponse(responseCode = "200", description = "Updated tenant", content = @Content(schema = @Schema(implementation = TenantResponse.class)))
    @APIResponse(responseCode = "404", description = "Tenant not found")
    public TenantResponse updateStatus(@PathParam("id") String id, @Valid TenantStatusUpdateRequest request) {
        Tenant tenant = Tenant.findById(id);
        if (tenant == null) {
            throw new NotFoundException("Tenant not found");
        }
        tenant.status = request.getStatus();
        return TenantResponse.from(tenant);
    }

    @PUT
    @Path("/{id}/threshold")
    @Transactional
    @Operation(summary = "Update approval threshold", description = "Update the SMS approval threshold for a tenant")
    @APIResponse(responseCode = "200", description = "Updated tenant", content = @Content(schema = @Schema(implementation = TenantResponse.class)))
    @APIResponse(responseCode = "404", description = "Tenant not found")
    public TenantResponse updateThreshold(@PathParam("id") String id, @Valid TenantThresholdUpdateRequest request) {
        Tenant tenant = Tenant.findById(id);
        if (tenant == null) {
            throw new NotFoundException("Tenant not found");
        }
        tenant.smsApprovalThreshold = request.getApprovalThreshold();
        return TenantResponse.from(tenant);
    }
}
