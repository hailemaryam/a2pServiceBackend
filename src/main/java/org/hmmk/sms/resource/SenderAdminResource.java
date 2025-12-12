package org.hmmk.sms.resource;

import io.quarkus.panache.common.Page;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.hmmk.sms.dto.common.PaginatedResponse;
import org.hmmk.sms.dto.SenderRejectRequest;
import org.hmmk.sms.dto.SenderResponse;
import org.hmmk.sms.entity.Sender;

import java.util.List;

/**
 * Admin resource for managing Sender approvals.
 * Only accessible by sys_admin role.
 */
@Path("/api/admin/senders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Sender Admin", description = "Admin operations for approving/rejecting SMS senders")
@RolesAllowed("sys_admin")
public class SenderAdminResource {

    @Inject
    JsonWebToken jwt;

    private String userIdFromJwt() {
        if (jwt == null)
            return null;
        return jwt.getSubject();
    }

    /**
     * List all senders pending verification with pagination.
     */
    @GET
    @Path("/pending")
    @Operation(summary = "List pending senders", description = "Get all senders that are pending verification")
    @APIResponse(responseCode = "200", description = "Paginated list of pending senders")
    public PaginatedResponse<SenderResponse> listPending(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        Page p = Page.of(page, size);

        var query = Sender.find("status", Sender.SenderStatus.PENDING_VERIFICATION).page(p);
        List<Sender> senders = query.list();
        long total = Sender.count("status", Sender.SenderStatus.PENDING_VERIFICATION);

        List<SenderResponse> items = senders.stream()
                .map(SenderResponse::fromEntity)
                .toList();

        return new PaginatedResponse<>(items, total, page, size);
    }

    /**
     * Get details of a specific sender.
     */
    @GET
    @Path("/{id}")
    @Operation(summary = "Get sender details", description = "Get details of a specific sender by ID")
    @APIResponse(responseCode = "200", description = "Sender details", content = @Content(schema = @Schema(implementation = SenderResponse.class)))
    @APIResponse(responseCode = "404", description = "Sender not found")
    public SenderResponse getById(@PathParam("id") String id) {
        Sender sender = Sender.findById(id);

        if (sender == null) {
            throw new NotFoundException("Sender not found");
        }

        return SenderResponse.fromEntity(sender);
    }

    /**
     * Approve a sender that is pending verification.
     */
    @POST
    @Path("/{id}/approve")
    @Transactional
    @Operation(summary = "Approve sender", description = "Approve a sender that is pending verification")
    @APIResponse(responseCode = "200", description = "Sender approved successfully", content = @Content(schema = @Schema(implementation = SenderResponse.class)))
    @APIResponse(responseCode = "400", description = "Sender is not pending verification")
    @APIResponse(responseCode = "404", description = "Sender not found")
    public SenderResponse approve(@PathParam("id") String id) {
        Sender sender = Sender.findById(id);

        if (sender == null) {
            throw new NotFoundException("Sender not found");
        }

        if (sender.getStatus() != Sender.SenderStatus.PENDING_VERIFICATION) {
            throw new BadRequestException("Sender is not pending verification. Current status: " + sender.getStatus());
        }

        sender.setStatus(Sender.SenderStatus.ACTIVE);
        sender.persist();

        return SenderResponse.fromEntity(sender, "Sender approved successfully");
    }

    /**
     * Reject a sender that is pending verification.
     */
    @POST
    @Path("/{id}/reject")
    @Transactional
    @Operation(summary = "Reject sender", description = "Reject a sender that is pending verification")
    @APIResponse(responseCode = "200", description = "Sender rejected", content = @Content(schema = @Schema(implementation = SenderResponse.class)))
    @APIResponse(responseCode = "400", description = "Sender is not pending verification")
    @APIResponse(responseCode = "404", description = "Sender not found")
    public SenderResponse reject(@PathParam("id") String id, SenderRejectRequest request) {
        Sender sender = Sender.findById(id);

        if (sender == null) {
            throw new NotFoundException("Sender not found");
        }

        if (sender.getStatus() != Sender.SenderStatus.PENDING_VERIFICATION) {
            throw new BadRequestException("Sender is not pending verification. Current status: " + sender.getStatus());
        }

        sender.setStatus(Sender.SenderStatus.REJECTED);
        sender.persist();

        String message = "Sender rejected";
        if (request != null && request.getReason() != null && !request.getReason().isBlank()) {
            message += ": " + request.getReason();
        }

        return SenderResponse.fromEntity(sender, message);
    }
}
