package org.hmmk.sms.resource;

import io.quarkus.panache.common.Page;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.hmmk.sms.dto.PaginatedResponse;
import org.hmmk.sms.dto.SenderCreateRequest;
import org.hmmk.sms.dto.SenderResponse;
import org.hmmk.sms.entity.Sender;

import java.util.List;

/**
 * Resource for tenant users to manage their SMS Senders.
 * Accessible by tenant_admin and tenant_user roles.
 */
@Path("/api/senders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Sender", description = "Manage SMS sender masks")
@RolesAllowed({ "tenant_admin", "tenant_user" })
public class SenderResource {

    @Inject
    JsonWebToken jwt;

    private String tenantIdFromJwt() {
        if (jwt == null)
            return null;
        Object claim = jwt.getClaim("tenantId");
        return claim == null ? null : claim.toString();
    }

    /**
     * Create a new sender. Status will be set to PENDING_VERIFICATION.
     */
    @POST
    @Transactional
    @Operation(summary = "Create sender", description = "Create a new SMS sender mask. Will require admin approval before use.")
    @APIResponse(responseCode = "200", description = "Sender created successfully", content = @Content(schema = @Schema(implementation = SenderResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid request data")
    public SenderResponse create(@Valid SenderCreateRequest request) {
        String tenantId = tenantIdFromJwt();

        Sender sender = Sender.builder()
                .name(request.getName())

                .status(Sender.SenderStatus.PENDING_VERIFICATION)
                .build();
        sender.tenantId = tenantId;
        sender.persist();

        return SenderResponse.fromEntity(sender, "Sender created and pending admin approval");
    }

    /**
     * List all senders for the current tenant with pagination.
     */
    @GET
    @Operation(summary = "List senders", description = "Get all senders for the current tenant with pagination")
    @APIResponse(responseCode = "200", description = "Paginated list of senders")
    public PaginatedResponse<SenderResponse> list(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        String tenantId = tenantIdFromJwt();
        Page p = Page.of(page, size);

        var query = Sender.find("tenantId", tenantId).page(p);
        List<Sender> senders = query.list();
        long total = Sender.count("tenantId", tenantId);

        List<SenderResponse> items = senders.stream()
                .map(SenderResponse::fromEntity)
                .toList();

        return new PaginatedResponse<>(items, total, page, size);
    }

    /**
     * Get a specific sender by ID.
     */
    @GET
    @Path("/{id}")
    @Operation(summary = "Get sender", description = "Get a specific sender by ID")
    @APIResponse(responseCode = "200", description = "Sender details", content = @Content(schema = @Schema(implementation = SenderResponse.class)))
    @APIResponse(responseCode = "404", description = "Sender not found")
    public SenderResponse getById(@PathParam("id") String id) {
        String tenantId = tenantIdFromJwt();
        Sender sender = Sender.findById(id);

        if (sender == null || !tenantId.equals(sender.tenantId)) {
            throw new NotFoundException("Sender not found");
        }

        return SenderResponse.fromEntity(sender);
    }

    /**
     * Update a sender. Only allowed if status is PENDING_VERIFICATION or REJECTED.
     */
    @PUT
    @Path("/{id}")
    @Transactional
    @Operation(summary = "Update sender", description = "Update a sender. Only allowed if status is PENDING_VERIFICATION or REJECTED.")
    @APIResponse(responseCode = "200", description = "Sender updated successfully", content = @Content(schema = @Schema(implementation = SenderResponse.class)))
    @APIResponse(responseCode = "400", description = "Cannot update sender in current status")
    @APIResponse(responseCode = "404", description = "Sender not found")
    public SenderResponse update(@PathParam("id") String id, @Valid SenderCreateRequest request) {
        String tenantId = tenantIdFromJwt();
        Sender sender = Sender.findById(id);

        if (sender == null || !tenantId.equals(sender.tenantId)) {
            throw new NotFoundException("Sender not found");
        }

        // Only allow updates if pending or rejected
        if (sender.getStatus() != Sender.SenderStatus.PENDING_VERIFICATION &&
                sender.getStatus() != Sender.SenderStatus.REJECTED) {
            throw new BadRequestException("Cannot update sender with status: " + sender.getStatus());
        }

        sender.setName(request.getName());

        sender.setStatus(Sender.SenderStatus.PENDING_VERIFICATION);
        sender.persist();

        return SenderResponse.fromEntity(sender, "Sender updated and pending admin approval");
    }

    /**
     * Delete a sender.
     */
    @DELETE
    @Path("/{id}")
    @Transactional
    @Operation(summary = "Delete sender", description = "Delete a sender")
    @APIResponse(responseCode = "204", description = "Sender deleted successfully")
    @APIResponse(responseCode = "404", description = "Sender not found")
    public void delete(@PathParam("id") String id) {
        String tenantId = tenantIdFromJwt();
        Sender sender = Sender.findById(id);

        if (sender == null || !tenantId.equals(sender.tenantId)) {
            throw new NotFoundException("Sender not found");
        }

        sender.delete();
    }
}
