package org.hmmk.sms.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.hmmk.sms.dto.SmsPackageTierDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
public class SmsPackageTierResourceTest {

    @Test
    @TestSecurity(user = "admin", roles = "sys_admin")
    public void testCreatePackage() {
        SmsPackageTierDto dto = new SmsPackageTierDto();
        dto.minSmsCount = 1000;
        dto.maxSmsCount = 5000;
        dto.pricePerSms = new BigDecimal("0.50");
        dto.description = "Bronze Package";
        dto.isActive = true;

        given()
                .contentType(ContentType.JSON)
                .body(dto)
                .when()
                .post("/api/admin/sms-packages")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("minSmsCount", is(1000));

        // Verify it appears in the list
        given()
                .when()
                .get("/api/admin/sms-packages")
                .then()
                .statusCode(200)
                .body("items.find { it.description == 'Bronze Package' }.minSmsCount", is(1000));
    }
    @Test
    public void testCreatePackageUnauthorized() {
        SmsPackageTierDto dto = new SmsPackageTierDto();
        dto.minSmsCount = 1000;

        given()
                .contentType(ContentType.JSON)
                .body(dto)
                .when()
                .post("/api/admin/sms-packages")
                .then()
                .statusCode(401);
    }

    @Test
    @TestSecurity(user = "user", roles = "tenant_admin")
    public void testCreatePackageForbidden() {
        SmsPackageTierDto dto = new SmsPackageTierDto();
        dto.minSmsCount = 1000;

        given()
                .contentType(ContentType.JSON)
                .body(dto)
                .when()
                .post("/api/admin/sms-packages")
                .then()
                .statusCode(403);
    }

    @Test
    public void testListPackages() {
        given()
                .when()
                .get("/api/admin/sms-packages")
                .then()
                .statusCode(200)
                .body("items", notNullValue());
    }
}
