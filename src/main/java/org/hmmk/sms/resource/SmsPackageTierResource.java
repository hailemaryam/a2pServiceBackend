package org.hmmk.sms.resource;

import jakarta.annotation.security.RolesAllowed;
import jakarta.annotation.security.PermitAll;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.hmmk.sms.dto.SmsPackageTierDto;
import org.hmmk.sms.dto.common.PaginatedResponse;
import org.hmmk.sms.entity.payment.SmsPackageTier;

import java.net.URI;
import java.util.List;

import io.quarkus.panache.common.Page;

@Path("/api/admin/sms-packages")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "SmsPackageTier")
public class SmsPackageTierResource {

    @POST
    @RolesAllowed("sys_admin")
    @Transactional
    public Response createPackage(@Valid SmsPackageTierDto dto) {
        SmsPackageTier tier = SmsPackageTier.builder()
                .minSmsCount(dto.minSmsCount)
                .maxSmsCount(dto.maxSmsCount)
                .pricePerSms(dto.pricePerSms)
                .description(dto.description)
                .isActive(dto.isActive)
                .build();
        // ensure id generation: PanacheEntityBase doesn't provide persist(), so use persist via entity manager via Panache
        tier.persist();
        return Response.created(URI.create("/api/admin/sms-packages/" + tier.id)).entity(tier).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("sys_admin")
    @Transactional
    public Response updatePackage(@PathParam("id") String id, @Valid SmsPackageTierDto dto) {
        SmsPackageTier tier = SmsPackageTier.findById(id);
        if (tier == null) {
            throw new NotFoundException("SmsPackageTier not found: " + id);
        }
        tier.minSmsCount = dto.minSmsCount;
        tier.maxSmsCount = dto.maxSmsCount;
        tier.pricePerSms = dto.pricePerSms;
        tier.description = dto.description;
        tier.isActive = dto.isActive;
        tier.persist();
        return Response.ok(tier).build();
    }

    @GET
    @PermitAll
    public PaginatedResponse<SmsPackageTier> listAll(@QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("20") int size) {
        Page p = Page.of(page, size);
        var query = SmsPackageTier.find("isActive = true").page(p);
        List<SmsPackageTier> items = query.list();
        long total = SmsPackageTier.count("isActive = true");
        return new PaginatedResponse<>(items, total, page, size);
    }

    @GET
    @Path("/{id}")
    @PermitAll
    public SmsPackageTier getById(@PathParam("id") String id) {
        SmsPackageTier tier = SmsPackageTier.findById(id);
        if (tier == null) throw new NotFoundException();
        return tier;
    }
}
