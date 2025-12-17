package org.hmmk.sms.resource;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.hmmk.sms.dto.RegistrationRequest;
import org.hmmk.sms.dto.TenantCreateRequest;
import org.hmmk.sms.service.KeycloakUserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;

@QuarkusTest
public class RegistrationResourceTest {

    @InjectMock
    KeycloakUserService keycloakUserService;

    @Test
    public void testRegisterTenantAndAdmin() {
        RegistrationRequest request = new RegistrationRequest();
        request.setTenantName("Test Tenant");
        request.setEmail("test@example.com");
        request.setPhone("+1234567890");
        request.setUsername("admin");
        request.setPassword("password");
        request.setFirstName("Admin");
        request.setLastName("User");

        Mockito.doNothing().when(keycloakUserService).createTenantAdminUser(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString());

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/register")
                .then()
                .statusCode(200)
                .body("tenantId", notNullValue());
    }

    @Test
    @TestSecurity(user = "user-123", roles = "user")
    public void testRegisterTenantForAuthenticatedUser() {
        TenantCreateRequest request = new TenantCreateRequest();
        request.setName("Auth Tenant");
        request.setPhone("+9876543210");

        Mockito.doNothing().when(keycloakUserService).assignTenantToUser(anyString(), anyString());

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/register/onboard")
                .then()
                .statusCode(200)
                .body("tenantId", notNullValue());
    }
}
