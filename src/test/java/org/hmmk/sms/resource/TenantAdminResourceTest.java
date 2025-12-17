package org.hmmk.sms.resource;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.hmmk.sms.dto.TenantStatusUpdateRequest;
import org.hmmk.sms.dto.TenantThresholdUpdateRequest;
import org.hmmk.sms.entity.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.transaction.Transactional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
public class TenantAdminResourceTest {

    @InjectMock
    JsonWebToken jwt;

    @BeforeEach
    @Transactional
    public void setup() {
        Tenant.deleteAll();
    }

    @Test
    @TestSecurity(user = "admin-user", roles = "sys_admin")
    public void testListTenants() {
        createTenant("Tenant A");
        createTenant("Tenant B");

        given()
                .when()
                .get("/api/admin/tenants")
                .then()
                .statusCode(200)
                .body("total", is(2))
                .body("items.size()", is(2));
    }

    @Test
    @TestSecurity(user = "admin-user", roles = "sys_admin")
    public void testGetTenantById() {
        Tenant tenant = createTenant("Detail Tenant");

        given()
                .when()
                .get("/api/admin/tenants/" + tenant.id)
                .then()
                .statusCode(200)
                .body("name", is("Detail Tenant"));
    }

    @Test
    @TestSecurity(user = "admin-user", roles = "sys_admin")
    public void testUpdateStatus() {
        Tenant tenant = createTenant("Status Tenant");

        TenantStatusUpdateRequest request = new TenantStatusUpdateRequest();
        request.setStatus(Tenant.TenantStatus.INACTIVE);

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put("/api/admin/tenants/" + tenant.id + "/status")
                .then()
                .statusCode(200)
                .body("status", is("INACTIVE"));
    }

    @Test
    @TestSecurity(user = "admin-user", roles = "sys_admin")
    public void testUpdateThreshold() {
        Tenant tenant = createTenant("Threshold Tenant");

        TenantThresholdUpdateRequest request = new TenantThresholdUpdateRequest();
        request.setApprovalThreshold(500);

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .put("/api/admin/tenants/" + tenant.id + "/threshold")
                .then()
                .statusCode(200)
                .body("smsApprovalThreshold", is(500));
    }

    @Test
    @TestSecurity(user = "admin-user", roles = "sys_admin")
    public void testGetTenantNotFound() {
        given()
                .when()
                .get("/api/admin/tenants/invalid-id")
                .then()
                .statusCode(404);
    }

    @Transactional
    Tenant createTenant(String name) {
        Tenant tenant = new Tenant();
        tenant.name = name;
        tenant.phone = "+251911" + System.currentTimeMillis() % 1000000;
        tenant.status = Tenant.TenantStatus.ACTIVE;
        tenant.smsCredit = 100;
        tenant.persist();
        return tenant;
    }
}
