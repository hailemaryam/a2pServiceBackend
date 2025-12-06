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
import org.hmmk.sms.dto.ContactDto;
import org.hmmk.sms.entity.contact.Contact;
import org.hmmk.sms.service.ContactImportService;

import java.io.InputStream;
import java.util.List;

@Path("/api/contacts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Contact")
public class ContactResource {

    @Inject
    ContactImportService importService;

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
    public Contact create(@Valid ContactDto dto) {
        String tenantId = tenantIdFromJwt();
        Contact c = new Contact();
        c.phone = dto.phone;
        c.name = dto.name;
        c.email = dto.email;
        c.tenantId = tenantId;
        c.persist();
        return c;
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("tenant_admin")
    @Transactional
    public Contact update(@PathParam("id") String id, @Valid ContactDto dto) {
        Contact c = Contact.findById(id);
        if (c == null) throw new NotFoundException();
        c.phone = dto.phone;
        c.name = dto.name;
        c.email = dto.email;
        c.persist();
        return c;
    }

    @GET
    @PermitAll
    public List<Contact> list(@QueryParam("tenantId") String tenantId) {
        if (tenantId == null) tenantId = tenantIdFromJwt();
        if (tenantId == null) return Contact.listAll();
        return Contact.list("tenantId", tenantId);
    }

    @GET
    @Path("/{id}")
    @PermitAll
    public Contact getById(@PathParam("id") String id) {
        Contact c = Contact.findById(id);
        if (c == null) throw new NotFoundException();
        return c;
    }

    @GET
    @Path("/search/by-phone")
    @PermitAll
    public Contact findByPhone(@QueryParam("phone") String phone, @QueryParam("tenantId") String tenantId) {
        if (tenantId == null) tenantId = tenantIdFromJwt();
        if (tenantId == null) return Contact.find("phone", phone).firstResult();
        return Contact.find("tenantId = ?1 and phone = ?2", tenantId, phone).firstResult();
    }

    @POST
    @Path("/upload")
    @Consumes(MediaType.TEXT_PLAIN)
    @RolesAllowed("tenant_admin")
    public List<Contact> uploadCsv(InputStream body, @QueryParam("groupId") String groupId) throws Exception {
        String tenantId = tenantIdFromJwt();
        return importService.importFromCsv(body, tenantId, groupId);
    }
}
