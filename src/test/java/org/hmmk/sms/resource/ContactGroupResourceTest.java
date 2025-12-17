package org.hmmk.sms.resource;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.hmmk.sms.entity.Tenant;
import org.hmmk.sms.entity.contact.ContactGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.transaction.Transactional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
public class ContactGroupResourceTest {

    @InjectMock
    JsonWebToken jwt;

    private String tenantId;

    @BeforeEach
    @Transactional
    public void setup() {
        ContactGroup.deleteAll();

        // Create a tenant with generated ID
        Tenant tenant = new Tenant();
        tenant.name = "ContactGroup Tenant " + System.currentTimeMillis();
        tenant.phone = "+0000000001";
        tenant.status = Tenant.TenantStatus.ACTIVE;
        tenant.persist();
        this.tenantId = tenant.id;
    }

    @Test
    @TestSecurity(user = "tenant-admin", roles = "tenant_admin")
    public void testCreateContactGroup() {
        Mockito.when(jwt.getClaim("tenantId")).thenReturn(tenantId);

        ContactGroup group = new ContactGroup();
        group.name = "Marketing";
        group.description = "Marketing Team";

        given()
                .contentType(ContentType.JSON)
                .body(group)
                .when()
                .post("/api/contact-groups")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("name", is("Marketing"))
                .body("tenantId", is(tenantId));
    }

    @Test
    @TestSecurity(user = "tenant-admin", roles = "tenant_admin")
    public void testListContactGroups() {
        Mockito.when(jwt.getClaim("tenantId")).thenReturn(tenantId);

        createGroup("Group A");
        createGroup("Group B");

        given()
                .when()
                .get("/api/contact-groups")
                .then()
                .statusCode(200)
                .body("total", is(2))
                .body("items[0].name", notNullValue());
    }

    @Transactional
    void createGroup(String name) {
        ContactGroup g = new ContactGroup();
        g.tenantId = tenantId;
        g.name = name;
        g.persist();
    }
}
