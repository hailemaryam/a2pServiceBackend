package org.hmmk.sms.resource;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.hmmk.sms.dto.SenderCreateRequest;
import org.hmmk.sms.entity.Sender;
import org.hmmk.sms.entity.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.transaction.Transactional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class SenderResourceTest {

    @InjectMock
    JsonWebToken jwt;

    private static String TENANT_ID = "";

    @BeforeEach
    @Transactional
    public void setup() {
        Sender.deleteAll();
        if (TENANT_ID != null && !TENANT_ID.isEmpty()) {
            Tenant.delete("id", TENANT_ID);
        }

        Tenant tenant = new Tenant();
        tenant.name = "Sender Test Tenant";
        tenant.phone = "+0000000000";
        tenant.status = Tenant.TenantStatus.ACTIVE;
        tenant.persist();
        TENANT_ID = tenant.id;
    }

    @Test
    @TestSecurity(user = "tenant-admin", roles = "tenant_admin")
    public void testCreateSender() {
        Mockito.when(jwt.getClaim("tenantId")).thenReturn(TENANT_ID);

        SenderCreateRequest request = new SenderCreateRequest();
        request.setName("MyCompany");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/senders")
                .then()
                .statusCode(200)
                .body("name", is("MyCompany"))
                .body("status", is("PENDING_VERIFICATION"));
    }

    @Test
    @TestSecurity(user = "tenant-admin", roles = "tenant_admin")
    public void testListSenders() {
        Mockito.when(jwt.getClaim("tenantId")).thenReturn(TENANT_ID);

        // Create a sender first
        createSender("Sender1", Sender.SenderStatus.ACTIVE);

        given()
                .when()
                .get("/api/senders")
                .then()
                .statusCode(200)
                .body("total", is(1))
                .body("items[0].name", is("Sender1"));
    }

    @Test
    @TestSecurity(user = "sys-admin", roles = "sys_admin")
    public void testApproveSender() {
        // Create a pending sender
        Sender sender = createSender("PendingSender", Sender.SenderStatus.PENDING_VERIFICATION);

        given()
                .contentType(ContentType.JSON)
                .when()
                .post("/api/admin/senders/" + sender.id + "/approve")
                .then()
                .statusCode(200)
                .body("status", is("ACTIVE"));
    }

    @Transactional
    Sender createSender(String name, Sender.SenderStatus status) {
        Sender sender = new Sender();
        sender.tenantId = TENANT_ID;
        sender.name = name;
        sender.status = status;
        sender.persist();
        return sender;
    }
}
