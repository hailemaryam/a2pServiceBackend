package org.hmmk.sms.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.hmmk.sms.dto.DashboardResponse;
import org.hmmk.sms.dto.SmsSentBySource;
import org.hmmk.sms.service.DashboardService;

@Path("/api/dashboard")
@Produces(MediaType.APPLICATION_JSON)
public class DashboardResource {

    @Inject
    JsonWebToken jwt;

    @Inject
    DashboardService dashboardService;

    private String tenantIdFromJwt() {
        if (jwt == null) return null;
        Object claim = jwt.getClaim("tenantId");
        return claim == null ? null : claim.toString();
    }

    @GET
    @RolesAllowed("tenant_admin")
    public DashboardResponse getDashboard() {
        String tenantId = tenantIdFromJwt();
        if (tenantId == null) {
            // return empty response when tenant not found in token
            return DashboardResponse.builder()
                    .remainingCredits(0)
                    .contactCount(0)
                    .smsSentBySource(SmsSentBySource.builder().api(0).manual(0).csvUpload(0).build())
                    .build();
        }
        return dashboardService.getDashboard(tenantId);
    }
}
