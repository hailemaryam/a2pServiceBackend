package org.hmmk.sms.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.hmmk.sms.dto.PaginatedResponse;
import org.hmmk.sms.dto.sms.SmsJobRejectRequest;
import org.hmmk.sms.dto.sms.SmsJobResponse;
import org.hmmk.sms.entity.sms.SmsJob;
import org.hmmk.sms.service.SmsJobService;

import java.util.UUID;

/**
 * Admin resource for SMS job management.
 * Only accessible by sys_admin role.
 */
@Path("/api/admin/sms-jobs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "SMS Admin", description = "SMS job administration - approval and management")
@RolesAllowed("sys_admin")
public class SmsJobAdminResource {

    @Inject
    JsonWebToken jwt;

    @Inject
    SmsJobService smsJobService;

    private String userIdFromJwt() {
        if (jwt == null)
            return null;
        return jwt.getSubject();
    }

    /**
     * List all SMS jobs pending approval with pagination.
     */
    @GET
    @Path("/pending")
    @Operation(summary = "List pending SMS jobs", description = "Get all SMS jobs that are pending approval")
    @APIResponse(responseCode = "200", description = "Paginated list of pending SMS jobs")
    public PaginatedResponse<SmsJobResponse> listPendingJobs(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        var result = smsJobService.listPendingApprovalJobs(page, size);
        var items = result.items().stream()
                .map(job -> SmsJobResponse.fromEntity(job, null))
                .toList();
        return new PaginatedResponse<>(items, result.total(), result.page(), result.size());
    }

    /**
     * Get details of a specific SMS job.
     */
    @GET
    @Path("/{jobId}")
    @Operation(summary = "Get SMS job details", description = "Get details of a specific SMS job by ID")
    @APIResponse(responseCode = "200", description = "SMS job details", content = @Content(schema = @Schema(implementation = SmsJobResponse.class)))
    @APIResponse(responseCode = "404", description = "SMS job not found")
    public SmsJobResponse getJobDetails(@PathParam("jobId") UUID jobId) {
        SmsJob job = smsJobService.getJobById(jobId);
        return SmsJobResponse.fromEntity(job, null);
    }

    /**
     * Approve an SMS job that is pending approval.
     */
    @POST
    @Path("/{jobId}/approve")
    @Operation(summary = "Approve SMS job", description = "Approve an SMS job that is pending approval")
    @APIResponse(responseCode = "200", description = "SMS job approved successfully", content = @Content(schema = @Schema(implementation = SmsJobResponse.class)))
    @APIResponse(responseCode = "400", description = "Job is not pending approval")
    @APIResponse(responseCode = "404", description = "SMS job not found")
    public SmsJobResponse approveJob(@PathParam("jobId") UUID jobId) {
        String adminUserId = userIdFromJwt();
        SmsJob job = smsJobService.approveJob(jobId, adminUserId);
        return SmsJobResponse.fromEntity(job, "SMS job approved and scheduled for sending");
    }

    /**
     * Reject an SMS job that is pending approval.
     */
    @POST
    @Path("/{jobId}/reject")
    @Operation(summary = "Reject SMS job", description = "Reject an SMS job that is pending approval")
    @APIResponse(responseCode = "200", description = "SMS job rejected", content = @Content(schema = @Schema(implementation = SmsJobResponse.class)))
    @APIResponse(responseCode = "400", description = "Job is not pending approval")
    @APIResponse(responseCode = "404", description = "SMS job not found")
    public SmsJobResponse rejectJob(@PathParam("jobId") UUID jobId, SmsJobRejectRequest request) {
        String adminUserId = userIdFromJwt();
        String reason = request != null ? request.getReason() : null;
        SmsJob job = smsJobService.rejectJob(jobId, adminUserId, reason);
        return SmsJobResponse.fromEntity(job, "SMS job rejected");
    }
}
