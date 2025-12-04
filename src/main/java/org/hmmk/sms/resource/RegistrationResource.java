package org.hmmk.sms.resource;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.hmmk.sms.dto.RegistrationRequest;
import org.hmmk.sms.entity.Tenant;
import org.hmmk.sms.service.KeycloakUserService;
import org.keycloak.admin.client.Keycloak;

import java.util.Map;

@Path("/api/register")
@SecurityRequirement(name = "keycloak")
public class RegistrationResource {

    @Inject
    KeycloakUserService keycloakAdminClient;

    @Inject
    Keycloak keycloak;

    @Inject
    JsonWebToken jwt;
    // test commit for pipeline
    @GET
    @Path("/me")
    public String getTenantId() {
        String tenantId = jwt.getClaim("tenantId");
        String preferredUsername = jwt.getClaim("preferred_username");
        return tenantId != null ? tenantId : "No tenantId found in token" + " (user: " + preferredUsername + ")";
    }

    @POST
    @PermitAll
    @Transactional
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerTenantAndAdmin(@Valid RegistrationRequest request) {
        // 1. Create Tenant
        Tenant tenant = new Tenant();
        tenant.name = request.getTenantName();
        tenant.domain = request.getDomain();
        tenant.status = Tenant.TenantStatus.ACTIVE;
        tenant.persist();

        // 2. Create Keycloak User
        keycloakAdminClient.createTenantAdminUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                tenant.id,
                request.getFirstName(),
                request.getLastName()
        );

        return Response.status(Response.Status.CREATED)
                .entity(Map.of("tenantId", tenant.id))
                .build();
    }
}
