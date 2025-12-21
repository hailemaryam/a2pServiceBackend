package org.hmmk.sms.resource;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.BadRequestException;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.hmmk.sms.dto.ContactDto;
import org.hmmk.sms.dto.common.PaginatedResponse;
import org.hmmk.sms.entity.contact.Contact;
import org.hmmk.sms.service.ContactImportService;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.jboss.resteasy.reactive.RestForm;

import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import io.quarkus.panache.common.Page;

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
    @RolesAllowed({"tenant_admin"})
    public PaginatedResponse<Contact> list(@QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("20") int size) {
        String tenantId = tenantIdFromJwt();
        Page p = Page.of(page, size);
        var query = Contact.find("tenantId", tenantId).page(p);
        List<Contact> items = query.list();
        long total = Contact.count("tenantId", tenantId);
        return new PaginatedResponse<>(items, total, page, size);
    }

    @GET
    @Path("/{id}")
    @PermitAll
    public Contact getById(@PathParam("id") String id) {
        Contact c = Contact.findById(id);
        if (c == null) throw new NotFoundException();
        return c;
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("tenant_admin")
    @Transactional
    public void delete(@PathParam("id") String id) {
        String tenantId = tenantIdFromJwt();
        String query = tenantId == null ? "id = ?1" : "id = ?1 and tenantId = ?2";
        Contact c = Contact.find(query, tenantId == null ? id : new Object[]{id, tenantId}).firstResult();
        if (c == null) throw new NotFoundException();
        c.delete();
    }

    @GET
    @Path("/search/by-phone")
    @RolesAllowed("tenant_admin")
    public Contact findByPhone(@QueryParam("phone") String phone) {
        String tenantId = tenantIdFromJwt();
        if (tenantId == null) return Contact.find("phone", phone).firstResult();
        return Contact.find("tenantId = ?1 and phone = ?2", tenantId, phone).firstResult();
    }

    @POST
    @Path("/upload-file")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RolesAllowed("tenant_admin")
    public List<Contact> uploadFile(@RestForm("file") FileUpload file, @QueryParam("groupId") String groupId) throws Exception {
        if (file == null) throw new BadRequestException("file missing");
        java.nio.file.Path uploaded = file.uploadedFile();
        if (uploaded == null) throw new BadRequestException("file missing");
        try (InputStream in = Files.newInputStream(uploaded)) {
            String tenantId = tenantIdFromJwt();
            return importService.importFromCsv(in, tenantId, groupId);
        }
    }

    @POST
    @Path("/upload")
    @Consumes({MediaType.TEXT_PLAIN, MediaType.APPLICATION_OCTET_STREAM})
    @RolesAllowed("tenant_admin")
    @RequestBody(description = "Upload CSV or Excel file as binary", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM, schema = @Schema(type = SchemaType.STRING, format = "binary")))
    public List<Contact> uploadCsv(InputStream body, @QueryParam("groupId") String groupId) throws Exception {
        String tenantId = tenantIdFromJwt();
        return importService.importFromCsv(body, tenantId, groupId);
    }

    /**
     * add contacts to a contact group.
     *
     */
    @POST
    @Path("/add-to-group/{groupId}")
    @RolesAllowed("tenant_admin")
    @Transactional
    public void addToGroup(@PathParam("groupId") String groupId, @QueryParam("contactIds") List<String> contactIds) {
        String tenantId = tenantIdFromJwt();
        if (tenantId == null || tenantId.isBlank()) {
            throw new BadRequestException("tenantId missing from JWT");
        }
        importService.addContactsToGroup(tenantId, groupId, contactIds);
    }
}
