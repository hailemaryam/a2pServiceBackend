package org.hmmk.sms.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.hmmk.sms.dto.AdminDashboardResponse;
import org.hmmk.sms.dto.DashboardOverviewResponse;
import org.hmmk.sms.dto.DashboardResponse;
import org.hmmk.sms.dto.SmsSentBySource;
import org.hmmk.sms.service.DashboardService;
import org.hmmk.sms.service.DashboardService.Granularity;

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

    @GET
    @Path("/admin")
    @RolesAllowed("sys_admin")
    public AdminDashboardResponse getAdminDashboard() {
        return dashboardService.getAdminDashboardOverview();
    }

    @GET
    @Path("/overview")
    @RolesAllowed("tenant_admin")
    public Response getSmsOverview(@QueryParam("granularity") String granularityParam) {
        String tenantId = tenantIdFromJwt();
        if (tenantId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Tenant ID missing in token")
                    .build();
        }

        Granularity granularity = Granularity.MONTH;
        if (granularityParam != null && !granularityParam.isBlank()) {
            try {
                granularity = Granularity.valueOf(granularityParam.toUpperCase());
            } catch (IllegalArgumentException ex) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Invalid granularity. Use MONTH, QUARTER, or YEAR.")
                        .build();
            }
        }

        DashboardOverviewResponse overview = dashboardService.getSmsOverview(tenantId, granularity);
        return Response.ok(overview).build();
    }
}
