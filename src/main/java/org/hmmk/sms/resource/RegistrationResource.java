package org.hmmk.sms.resource;

import io.quarkus.security.Authenticated;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.hmmk.sms.dto.RegistrationRequest;
import org.hmmk.sms.dto.TenantCreateRequest;
import org.hmmk.sms.entity.Tenant;
import org.hmmk.sms.service.KeycloakUserService;
import org.keycloak.admin.client.Keycloak;

import java.util.Map;

@Path("/api/register")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Tenant Registration")
public class RegistrationResource {

    @Inject
    KeycloakUserService keycloakAdminClient;

    @Inject
    Keycloak keycloak;

    @Inject
    JsonWebToken jwt;

    @POST
    @PermitAll
    @Transactional
    public Response registerTenantAndAdmin(@Valid RegistrationRequest request) {
        // 1. Create Tenant
        Tenant tenant = new Tenant();
        tenant.name = request.getTenantName();
        tenant.email = request.getEmail();
        tenant.phone = request.getPhone();
        tenant.status = Tenant.TenantStatus.ACTIVE;
        tenant.persist();

        // 2. Create Keycloak User
        keycloakAdminClient.createTenantAdminUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                tenant.id,
                request.getFirstName(),
                request.getLastName());

        return Response.status(Response.Status.CREATED)
                .entity(Map.of("tenantId", tenant.id))
                .build();
    }

    @POST
    @Path("/onboard")
    @Authenticated
    @Transactional
    public Response registerTenantForAuthenticatedUser(@Valid TenantCreateRequest request) {
        String userId = jwt.getSubject();

        // Check if user already has tenantId claim (optional but good practice)
        // String existingTenantId = jwt.getClaim("tenantId");

        // 1. Create Tenant
        Tenant tenant = new Tenant();
        tenant.name = request.getName();
        // Email is not in request, maybe use jwt email if needed, or leave
        // null/optional
        // tenant.email = jwt.getClaim("email");
        tenant.phone = request.getPhone();
        tenant.status = Tenant.TenantStatus.ACTIVE;
        tenant.persist();

        // 2. Assign Tenant to User in Keycloak
        keycloakAdminClient.assignTenantToUser(userId, tenant.id);

        return Response.status(Response.Status.CREATED)
                .entity(Map.of("tenantId", tenant.id, "message", "Tenant created. Please refresh token."))
                .build();
    }
}
