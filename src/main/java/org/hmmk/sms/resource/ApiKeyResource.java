package org.hmmk.sms.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.hmmk.sms.dto.apikey.ApiKeyRequest;
import org.hmmk.sms.dto.apikey.ApiKeyResponse;
import org.hmmk.sms.dto.PaginatedResponse;
import org.hmmk.sms.entity.ApiKey;
import org.hmmk.sms.entity.Sender;

import java.util.List;
import java.util.stream.Collectors;

@Path("/api/api-keys")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "API Keys", description = "Manage API Keys for SMS sending")
public class ApiKeyResource {

    @Inject
    JsonWebToken jwt;

    private String tenantIdFromJwt() {
        if (jwt == null)
            return null;
        Object claim = jwt.getClaim("tenantId");
        return claim == null ? null : claim.toString();
    }

    @GET
    @RolesAllowed({ "tenant_admin", "tenant_user" })
    @Operation(summary = "List API Keys", description = "List all API keys for the current tenant with pagination")
    public PaginatedResponse<ApiKeyResponse> list(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        String tenantId = tenantIdFromJwt();

        var query = ApiKey.find("tenantId", tenantId).page(io.quarkus.panache.common.Page.of(page, size));
        List<ApiKeyResponse> items = query.stream()
                .map(apiKey -> ApiKeyResponse.fromEntity((ApiKey) apiKey))
                .collect(Collectors.toList());
        long total = query.count();

        return new PaginatedResponse<>(items, total, page, size);
    }

    @POST
    @RolesAllowed({ "tenant_admin" })
    @Operation(summary = "Create API Key", description = "Create a new API key for a specific sender")
    @Transactional
    public ApiKeyResponse create(@Valid ApiKeyRequest request) {
        String tenantId = tenantIdFromJwt();

        Sender sender = Sender.find("id = ?1 and tenantId = ?2", request.getSenderId(), tenantId).firstResult();
        if (sender == null) {
            throw new NotFoundException("Sender not found");
        }
        if (sender.status != Sender.SenderStatus.ACTIVE) {
            throw new BadRequestException("Sender is not active");
        }

        ApiKey apiKey = ApiKey.builder()
                .sender(sender)
                .name(request.getName())
                .build();
        apiKey.tenantId = tenantId;
        apiKey.persist();

        return ApiKeyResponse.fromEntity(apiKey);
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({ "tenant_admin" })
    @Operation(summary = "Revoke API Key", description = "Revoke (delete) an API key")
    @Transactional
    public void delete(@PathParam("id") String id) {
        String tenantId = tenantIdFromJwt();
        ApiKey apiKey = ApiKey.find("id = ?1 and tenantId = ?2", id, tenantId).firstResult();
        if (apiKey == null) {
            throw new NotFoundException("API Key not found");
        }
        apiKey.delete();
    }
}
