package org.hmmk.sms.resource;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.hmmk.sms.dto.sms.GroupSmsRequest;
import org.hmmk.sms.dto.sms.SingleSmsRequest;
import org.hmmk.sms.entity.sms.SmsJob;
import org.hmmk.sms.service.SmsJobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.transaction.Transactional;

import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@QuarkusTest
public class SmsJobResourceTest {

    @InjectMock
    JsonWebToken jwt;

    @InjectMock
    SmsJobService smsJobService;

    // Using a fixed tenant ID here for simplicity as we mock the service completely
    private static final String TENANT_ID = "tenant-123";
    private static final String USER_ID = "user-123";

    @BeforeEach
    public void setup() {
        // No DB setup needed as we mock the service
    }

    @Test
    @TestSecurity(user = "tenant-admin", roles = "tenant_admin")
    public void testSendSingleSms() {
        Mockito.when(jwt.getClaim("tenantId")).thenReturn(TENANT_ID);
        Mockito.when(jwt.getSubject()).thenReturn(USER_ID);

        SingleSmsRequest request = new SingleSmsRequest();
        request.setSenderId("MySender");
        request.setPhoneNumber("+251911000000");
        request.setMessage("Hello World");

        SmsJob mockJob = new SmsJob();
        mockJob.id = UUID.randomUUID();
        mockJob.jobType = SmsJob.JobType.SINGLE;
        mockJob.status = SmsJob.JobStatus.SCHEDULED;
        mockJob.approvalStatus = SmsJob.ApprovalStatus.APPROVED;

        // Mock the service call. Note: sendSingle has 3 args in some implementations, 4
        // in others (webhook).
        // The resource calls: smsJobService.sendSingle(tenantId, userId, request) which
        // calls the 4-arg version with null.
        // We mock the 3-arg version if the resource calls it directly, or matching
        // parameters.
        // Checking SmsJobResource: calls smsJobService.sendSingle(tenantId, userId,
        // request);
        // Checking SmsJobService: sendSingle(t,u,r) calls sendSingle(t,u,r,null).
        // Since we inject mock the service, we should mock the method that is actually
        // CALLED by the resource.
        Mockito.when(smsJobService.sendSingle(eq(TENANT_ID), eq(USER_ID), any(SingleSmsRequest.class)))
                .thenReturn(mockJob);

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/sms/single")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("message", is("Single SMS job created successfully"));
    }

    @Test
    @TestSecurity(user = "tenant-admin", roles = "tenant_admin")
    public void testSendGroupSms() {
        Mockito.when(jwt.getClaim("tenantId")).thenReturn(TENANT_ID);
        Mockito.when(jwt.getSubject()).thenReturn(USER_ID);

        GroupSmsRequest request = new GroupSmsRequest();
        request.setSenderId("MySender");
        request.setGroupId("group-123");
        request.setMessage("Hello Group");

        SmsJob mockJob = new SmsJob();
        mockJob.id = UUID.randomUUID();
        mockJob.jobType = SmsJob.JobType.GROUP;
        // Case: Pending Approval
        mockJob.approvalStatus = SmsJob.ApprovalStatus.PENDING;
        mockJob.status = SmsJob.JobStatus.PENDING_APPROVAL;

        Mockito.when(smsJobService.sendToGroup(eq(TENANT_ID), eq(USER_ID), any(GroupSmsRequest.class)))
                .thenReturn(mockJob);

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/sms/group")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("message", is("Group SMS job created and pending approval"));
    }

    @Test
    @TestSecurity(user = "tenant-admin", roles = "tenant_admin")
    public void testSendBulkSms() {
        Mockito.when(jwt.getClaim("tenantId")).thenReturn(TENANT_ID);
        Mockito.when(jwt.getSubject()).thenReturn(USER_ID);

        SmsJob mockJob = new SmsJob();
        mockJob.id = UUID.randomUUID();
        mockJob.jobType = SmsJob.JobType.BULK;
        mockJob.approvalStatus = SmsJob.ApprovalStatus.APPROVED;
        mockJob.status = SmsJob.JobStatus.SCHEDULED;

        Mockito.when(smsJobService.sendBulk(eq(TENANT_ID), eq(USER_ID), any(InputStream.class), any()))
                .thenReturn(mockJob);

        given()
                .contentType(ContentType.MULTIPART)
                .multiPart("file", "test.csv", "phone\n+251911223344".getBytes())
                .formParam("senderId", "MySender")
                .formParam("message", "Hello Bulk")
                .when()
                .post("/api/sms/bulk")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("message", is("Bulk SMS job created and scheduled"));
    }
}
