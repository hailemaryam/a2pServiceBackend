package org.hmmk.sms.resource;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.hmmk.sms.dto.sms.PublicSmsRequest;
import org.hmmk.sms.dto.sms.SingleSmsRequest;
import org.hmmk.sms.dto.sms.SmsJobResponse;
import org.hmmk.sms.entity.ApiKey;
import org.hmmk.sms.entity.sms.SmsJob;
import org.hmmk.sms.service.SmsJobService;

@Path("/api/p/sms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Public SMS", description = "Public SMS sending operations via API Key")
public class PublicSmsResource {

    @Inject
    SmsJobService smsJobService;

    @POST
    @Path("/send")
    @Operation(summary = "Send SMS", description = "Send an SMS using JSON body with optional scheduling and webhook")
    public SmsJobResponse sendPost(
            @HeaderParam("API-Key") String apiKeyStr,
            @Valid PublicSmsRequest request) {

        validateApiKey(apiKeyStr);
        return processSms(apiKeyStr, request);
    }

    private void validateApiKey(String apiKeyStr) {
        if (apiKeyStr == null || apiKeyStr.isBlank()) {
            throw new NotAuthorizedException("API Key is required");
        }
    }

    private SmsJobResponse processSms(String apiKeyStr, PublicSmsRequest request) {
        ApiKey apiKey = ApiKey.find("apiKey", apiKeyStr).firstResult();
        if (apiKey == null) {
            throw new NotAuthorizedException("Invalid API Key");
        }

        // Ensure sender is still active
        if (apiKey.sender.status != org.hmmk.sms.entity.Sender.SenderStatus.ACTIVE) {
            throw new BadRequestException("Associated sender is not active");
        }

        SingleSmsRequest internalRequest = SingleSmsRequest.builder()
                .senderId(apiKey.sender.id)
                .phoneNumber(request.getTo())
                .message(request.getMessage())
                .scheduledAt(request.getScheduledAt())
                .build();

        // Use "API-KEY:<id>" as the userId to trace source
        String userId = "API-KEY:" + apiKey.id;

        SmsJob job = smsJobService.sendSingle(apiKey.tenantId, userId, internalRequest, request.getWebhookUrl());

        return SmsJobResponse.fromEntity(job, "SMS sent successfully");
    }
}
