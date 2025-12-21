package org.hmmk.sms.resource;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.hmmk.sms.dto.common.PaginatedResponse;
import org.hmmk.sms.entity.contact.ContactGroup;

import java.util.List;
import io.quarkus.panache.common.Page;

@Path("/api/contact-groups")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "ContactGroup")
public class ContactGroupResource {

    @Inject
    JsonWebToken jwt;

    private String tenantIdFromJwt() {
        if (jwt == null) return null;
        Object claim = jwt.getClaim("tenantId");
        return claim == null ? null : claim.toString();
    }

    @POST
    @RolesAllowed("tenant_admin")
    @Transactional
    public ContactGroup create(@Valid ContactGroup group) {
        group.tenantId = tenantIdFromJwt();
        group.persist();
        return group;
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("tenant_admin")
    @Transactional
    public ContactGroup update(@PathParam("id") String id, @Valid ContactGroup payload) {
        String tenantId = tenantIdFromJwt();
        ContactGroup g = ContactGroup.findById(id);
        if (g.tenantId == null || !g.tenantId.equals(tenantId)) {
            throw new NotFoundException();
        }
        if (g == null) throw new NotFoundException();
        g.name = payload.name;
        g.description = payload.description;
        g.persist();
        return g;
    }

    @GET
    @RolesAllowed("tenant_admin")
    public PaginatedResponse<ContactGroup> list(@QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("20") int size) {
        String tenantId = tenantIdFromJwt();
        Page p = Page.of(page, size);
        var query = ContactGroup.find("tenantId", tenantId).page(p);
        List<ContactGroup> items = query.list();
        long total = ContactGroup.count("tenantId", tenantId);
        return new PaginatedResponse<>(items, total, page, size);
    }

    @GET
    @Path("/{id}")
    @PermitAll
    public ContactGroup getById(@PathParam("id") String id) {
        ContactGroup g = ContactGroup.findById(id);
        if (g == null) throw new NotFoundException();
        return g;
    }

    /**
     * Delete a contact group by ID.
     */
    @DELETE
    @Path("/{id}")
    @RolesAllowed("tenant_admin")
    @Transactional
    public void delete(@PathParam("id") String id) {
        String tenantId = tenantIdFromJwt();
        String query = "DELETE FROM ContactGroup WHERE id = ?1 AND tenantId = ?2";
        ContactGroup g = ContactGroup.find("id = ?1 and tenantId = ?2", id, tenantId).firstResult();
        if (g == null) throw new NotFoundException();
        g.delete();
    }
}
