package org.hmmk.sms.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.hmmk.sms.dto.sms.BulkSmsRequest;
import org.hmmk.sms.dto.sms.GroupSmsRequest;
import org.hmmk.sms.dto.sms.SingleSmsRequest;
import org.hmmk.sms.dto.sms.SmsJobResponse;
import org.hmmk.sms.entity.sms.SmsJob;
import org.hmmk.sms.service.SmsJobService;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;

/**
 * REST resource for SMS sending operations.
 */
@Path("/api/sms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "SMS", description = "SMS sending operations")
public class SmsJobResource {

    @Inject
    JsonWebToken jwt;

    @Inject
    SmsJobService smsJobService;

    private String tenantIdFromJwt() {
        if (jwt == null)
            return null;
        Object claim = jwt.getClaim("tenantId");
        return claim == null ? null : claim.toString();
    }

    private String userIdFromJwt() {
        if (jwt == null)
            return null;
        return jwt.getSubject();
    }

    /**
     * Send a single SMS to one phone number.
     */
    @POST
    @Path("/single")
    @RolesAllowed("tenant_admin")
    @Operation(summary = "Send single SMS", description = "Send an SMS to a single phone number")
    @APIResponse(responseCode = "200", description = "SMS job created successfully", content = @Content(schema = @Schema(implementation = SmsJobResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid request")
    @APIResponse(responseCode = "404", description = "Sender not found")
    public SmsJobResponse sendSingle(@Valid SingleSmsRequest request) {
        String tenantId = tenantIdFromJwt();
        String userId = userIdFromJwt();

        SmsJob job = smsJobService.sendSingle(tenantId, userId, request);

        return SmsJobResponse.fromEntity(job, "Single SMS job created successfully");
    }

    /**
     * Send SMS to all contacts in a contact group.
     */
    @POST
    @Path("/group")
    @RolesAllowed("tenant_admin")
    @Operation(summary = "Send group SMS", description = "Send an SMS to all contacts in a contact group")
    @APIResponse(responseCode = "200", description = "SMS job created successfully", content = @Content(schema = @Schema(implementation = SmsJobResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid request or empty group")
    @APIResponse(responseCode = "404", description = "Contact group or sender not found")
    public SmsJobResponse sendToGroup(@Valid GroupSmsRequest request) {
        String tenantId = tenantIdFromJwt();
        String userId = userIdFromJwt();

        SmsJob job = smsJobService.sendToGroup(tenantId, userId, request);

        String message = job.approvalStatus == SmsJob.ApprovalStatus.PENDING
                ? "Group SMS job created and pending approval"
                : "Group SMS job created and scheduled";

        return SmsJobResponse.fromEntity(job, message);
    }

    /**
     * Send bulk SMS from a CSV file containing phone numbers.
     */
    @POST
    @Path("/bulk")
    @RolesAllowed("tenant_admin")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Send bulk SMS", description = "Send SMS to phone numbers from a CSV file")
    @APIResponse(responseCode = "200", description = "SMS job created successfully", content = @Content(schema = @Schema(implementation = SmsJobResponse.class)))
    @APIResponse(responseCode = "400", description = "Invalid request or CSV file")
    @APIResponse(responseCode = "404", description = "Sender not found")
    public SmsJobResponse sendBulk(
            @RestForm("senderId") String senderId,
            @RestForm("message") String message,
            @RestForm("scheduledAt") String scheduledAtStr,
            @RestForm("file") FileUpload file) {

        String tenantId = tenantIdFromJwt();
        String userId = userIdFromJwt();

        if (senderId == null || senderId.isBlank()) {
            throw new BadRequestException("Sender ID is required");
        }
        if (message == null || message.isBlank()) {
            throw new BadRequestException("Message content is required");
        }
        if (file == null) {
            throw new BadRequestException("CSV file is required");
        }

        Instant scheduledAt = null;
        if (scheduledAtStr != null && !scheduledAtStr.isBlank()) {
            try {
                scheduledAt = Instant.parse(scheduledAtStr);
            } catch (Exception e) {
                throw new BadRequestException("Invalid scheduledAt format. Use ISO-8601 format.");
            }
        }

        BulkSmsRequest request = BulkSmsRequest.builder()
                .senderId(senderId)
                .message(message)
                .scheduledAt(scheduledAt)
                .build();

        try (InputStream inputStream = new FileInputStream(file.uploadedFile().toFile())) {
            SmsJob job = smsJobService.sendBulk(tenantId, userId, inputStream, request);

            String responseMessage = job.approvalStatus == SmsJob.ApprovalStatus.PENDING
                    ? "Bulk SMS job created and pending approval"
                    : "Bulk SMS job created and scheduled";

            return SmsJobResponse.fromEntity(job, responseMessage);
        } catch (IOException e) {
            throw new BadRequestException("Failed to read CSV file: " + e.getMessage());
        }
    }
    /**
     * list all SMS jobs for the current tenant with pagination.
     */
    @GET
    @RolesAllowed("tenant_admin")
    @Operation(summary = "List SMS jobs", description = "Get all SMS jobs for the current tenant with pagination")
    public org.hmmk.sms.dto.common.PaginatedResponse<SmsJob> list(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        String tenantId = tenantIdFromJwt();
        var query = SmsJob.find("tenantId", tenantId).page(io.quarkus.panache.common.Page.of(page, size));
        List<SmsJob> items = query.list();
        long total = SmsJob.count("tenantId", tenantId);
        return new org.hmmk.sms.dto.common.PaginatedResponse<>(items, total, page, size);
    }
    /**
     * Get pending SMS jobs that require approval.
     */
    @GET
    @Path("/pending-approvals")
    @RolesAllowed("tenant_admin")
    @Operation(summary = "Get pending SMS jobs for approval", description = "Retrieve SMS jobs that are pending approval")
    public org.hmmk.sms.dto.common.PaginatedResponse<SmsJob> getPendingApprovals(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size
    ) {
        String tenantId = tenantIdFromJwt();
        var query = SmsJob.find("tenantId = ?1 and approvalStatus = ?2", tenantId, SmsJob.ApprovalStatus.PENDING);
        List<SmsJob> items = query.list();
        long total = items.size();
        return new org.hmmk.sms.dto.common.PaginatedResponse<>(items, total, page, size);
    }
}
