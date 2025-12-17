package org.hmmk.sms.resource;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.hmmk.sms.dto.sms.PublicSmsRequest;
import org.hmmk.sms.dto.sms.SingleSmsRequest;
import org.hmmk.sms.entity.ApiKey;
import org.hmmk.sms.entity.Sender;
import org.hmmk.sms.entity.Tenant;
import org.hmmk.sms.entity.sms.SmsJob;
import org.hmmk.sms.service.SmsJobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.transaction.Transactional;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

@QuarkusTest
public class PublicSmsResourceTest {

    @InjectMock
    SmsJobService smsJobService;

    private String apiKeyStr;
    private String tenantId;

    @BeforeEach
    @Transactional
    public void setup() {
        ApiKey.deleteAll();
        Sender.deleteAll();

        Tenant tenant = new Tenant();
        tenant.name = "PublicSms Tenant " + System.currentTimeMillis();
        tenant.phone = "+0000000005";
        tenant.status = Tenant.TenantStatus.ACTIVE;
        tenant.persist();
        this.tenantId = tenant.id;

        Sender sender = new Sender();
        sender.tenantId = tenantId;
        sender.name = "MySender";
        sender.status = Sender.SenderStatus.ACTIVE;
        sender.persist();

        ApiKey apiKey = ApiKey.builder()
                .sender(sender)
                .name("Test Key")
                .build();
        apiKey.tenantId = tenantId;
        apiKey.persist();
        this.apiKeyStr = apiKey.apiKey;
    }

    @Test
    public void testSendSmsValidKey() {
        PublicSmsRequest request = new PublicSmsRequest();
        request.setTo("+251911000000");
        request.setMessage("Hello Public");

        SmsJob mockJob = new SmsJob();
        mockJob.id = UUID.randomUUID();
        mockJob.jobType = SmsJob.JobType.SINGLE;
        mockJob.status = SmsJob.JobStatus.SCHEDULED;

        Mockito.when(smsJobService.sendSingle(eq(tenantId), anyString(), any(SingleSmsRequest.class), any()))
                .thenReturn(mockJob);

        given()
                .contentType(ContentType.JSON)
                .header("API-Key", apiKeyStr)
                .body(request)
                .when()
                .post("/api/p/sms/send")
                .then()
                .statusCode(200)
                .body("id", notNullValue());
    }

    @Test
    public void testSendSmsInvalidKey() {
        PublicSmsRequest request = new PublicSmsRequest();
        request.setTo("+251911000000");
        request.setMessage("Hello Public");

        given()
                .contentType(ContentType.JSON)
                .header("API-Key", "invalid-key")
                .body(request)
                .when()
                .post("/api/p/sms/send")
                .then()
                .statusCode(401);
    }
}
