package org.hmmk.sms.resource;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.hmmk.sms.dto.ContactDto;
import org.hmmk.sms.entity.Tenant;
import org.hmmk.sms.entity.contact.Contact;
import org.hmmk.sms.service.ContactImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.transaction.Transactional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
public class ContactResourceTest {

    @InjectMock
    JsonWebToken jwt;

    @InjectMock
    ContactImportService importService;

    private String tenantId;

    @BeforeEach
    @Transactional
    public void setup() {
        Contact.deleteAll();

        Tenant tenant = new Tenant();
        tenant.name = "Contact Tenant " + System.currentTimeMillis();
        tenant.phone = "+0000000002";
        tenant.status = Tenant.TenantStatus.ACTIVE;
        tenant.persist();
        this.tenantId = tenant.id;
    }

    @Test
    @TestSecurity(user = "tenant-admin", roles = "tenant_admin")
    public void testCreateContact() {
        Mockito.when(jwt.getClaim("tenantId")).thenReturn(tenantId);

        ContactDto dto = new ContactDto();
        dto.name = "John Doe";
        dto.phone = "+251911000000";
        dto.email = "john@example.com";

        given()
                .contentType(ContentType.JSON)
                .body(dto)
                .when()
                .post("/api/contacts")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("name", is("John Doe"))
                .body("tenantId", is(tenantId));
    }

    @Test
    @TestSecurity(user = "tenant-admin", roles = "tenant_admin")
    public void testListContacts() {
        Mockito.when(jwt.getClaim("tenantId")).thenReturn(tenantId);

        createContact("C1", "+251911000001");
        createContact("C2", "+251911000002");

        given()
                .when()
                .get("/api/contacts")
                .then()
                .statusCode(200)
                .body("total", is(2))
                .body("items[0].name", notNullValue());
    }

    @Test
    @TestSecurity(user = "tenant-admin", roles = "tenant_admin")
    public void testSearchByPhone() {
        Mockito.when(jwt.getClaim("tenantId")).thenReturn(tenantId);
        String phone = "+251911000001";
        createContact("C1", phone);

        given()
                .queryParam("phone", phone)
                .when()
                .get("/api/contacts/search/by-phone")
                .then()
                .statusCode(200)
                .body("name", is("C1"));
    }

    @Transactional
    void createContact(String name, String phone) {
        Contact c = new Contact();
        c.tenantId = tenantId;
        c.name = name;
        c.phone = phone;
        c.persist();
    }
}
