package org.hmmk.sms.resource;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.hmmk.sms.dto.RegistrationRequest;
import org.hmmk.sms.entity.Tenant;
import org.hmmk.sms.service.KeycloakUserService;
import org.keycloak.admin.client.Keycloak;

import java.util.Map;

@Path("/api/register")
public class RegistrationResource {

    @Inject
    KeycloakUserService keycloakAdminClient;

    @Inject
    Keycloak keycloak;

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
                request.getAdminUsername(),
                request.getAdminEmail(),
                request.getAdminPassword(),
                tenant.id
        );

        return Response.status(Response.Status.CREATED)
                .entity(Map.of("tenantId", tenant.id))
                .build();
    }
}
