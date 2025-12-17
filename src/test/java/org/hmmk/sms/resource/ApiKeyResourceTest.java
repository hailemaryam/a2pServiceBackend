package org.hmmk.sms.resource;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.hmmk.sms.dto.apikey.ApiKeyRequest;
import org.hmmk.sms.entity.ApiKey;
import org.hmmk.sms.entity.Sender;
import org.hmmk.sms.entity.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.transaction.Transactional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
public class ApiKeyResourceTest {

    @InjectMock
    JsonWebToken jwt;

    private String tenantId;
    private String senderId;

    @BeforeEach
    @Transactional
    public void setup() {
        ApiKey.deleteAll();
        Sender.deleteAll();

        Tenant tenant = new Tenant();
        tenant.name = "ApiKey Tenant " + System.currentTimeMillis();
        tenant.phone = "+0000000004";
        tenant.status = Tenant.TenantStatus.ACTIVE;
        tenant.persist();
        this.tenantId = tenant.id;

        Sender sender = new Sender();
        sender.tenantId = tenantId;
        sender.name = "MySender";
        sender.status = Sender.SenderStatus.ACTIVE;
        sender.persist();
        this.senderId = sender.id;
    }

    @Test
    @TestSecurity(user = "tenant-admin", roles = "tenant_admin")
    public void testCreateApiKey() {
        Mockito.when(jwt.getClaim("tenantId")).thenReturn(tenantId);

        ApiKeyRequest request = new ApiKeyRequest();
        request.setSenderId(senderId);
        request.setName("Test Key");

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/api-keys")
                .then()
                .statusCode(200)
                .body("apiKey", notNullValue())
                .body("name", is("Test Key"));
    }

    @Test
    @TestSecurity(user = "tenant-admin", roles = "tenant_admin")
    public void testListApiKeys() {
        Mockito.when(jwt.getClaim("tenantId")).thenReturn(tenantId);

        createApiKey("Key 1");
        createApiKey("Key 2");

        System.out.println("DEBUG: TenantID=" + tenantId);
        System.out.println("DEBUG: ApiKey Count=" + ApiKey.count("tenantId", tenantId));

        given()
                .when()
                .get("/api/api-keys")
                .then()
                .statusCode(200)
                .body("total", is(2))
                .body("items[0].name", notNullValue());
    }

    @Test
    @TestSecurity(user = "tenant-admin", roles = "tenant_admin")
    public void testDeleteApiKey() {
        Mockito.when(jwt.getClaim("tenantId")).thenReturn(tenantId);

        ApiKey key = createApiKey("Key To Delete");

        given()
                .when()
                .delete("/api/api-keys/" + key.id)
                .then()
                .statusCode(204);

        // Verify deletion
        given()
                .when()
                .get("/api/api-keys")
                .then()
                .statusCode(200)
                .body("total", is(0));
    }

    ApiKey createApiKey(String name) {
        ApiKey key = ApiKey.builder()
                .sender(Sender.findById(senderId))
                .name(name)
                .build();
        key.tenantId = tenantId;
        QuarkusTransaction.requiringNew().run(key::persist);
        return key;
    }
}
