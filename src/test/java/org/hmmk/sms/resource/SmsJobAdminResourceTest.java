package org.hmmk.sms.resource;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.hmmk.sms.dto.sms.SmsJobRejectRequest;
import org.hmmk.sms.entity.sms.SmsJob;
import org.hmmk.sms.service.SmsJobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@QuarkusTest
public class SmsJobAdminResourceTest {

    @InjectMock
    JsonWebToken jwt;

    @InjectMock
    SmsJobService smsJobService;

    private static final String ADMIN_ID = "admin-user";

    @BeforeEach
    public void setup() {
        // Mock service calls
    }

    @Test
    @TestSecurity(user = "admin-user", roles = "sys_admin")
    public void testListPendingJobs() {
        SmsJob job = new SmsJob();
        job.id = UUID.randomUUID();
        job.status = SmsJob.JobStatus.PENDING_APPROVAL;

        SmsJobService.PaginatedResult<SmsJob> result = new SmsJobService.PaginatedResult<>(
                Collections.singletonList(job), 1, 0, 20);

        Mockito.when(smsJobService.listPendingApprovalJobs(0, 20)).thenReturn(result);

        given()
                .when()
                .get("/api/admin/sms-jobs/pending")
                .then()
                .statusCode(200)
                .body("total", is(1))
                .body("items[0].id", is(job.id.toString()));
    }

    @Test
    @TestSecurity(user = "admin-user", roles = "sys_admin")
    public void testApproveJob() {
        Mockito.when(jwt.getSubject()).thenReturn(ADMIN_ID);

        UUID jobId = UUID.randomUUID();
        SmsJob job = new SmsJob();
        job.id = jobId;
        job.approvalStatus = SmsJob.ApprovalStatus.APPROVED;
        job.status = SmsJob.JobStatus.SCHEDULED;

        Mockito.when(smsJobService.approveJob(jobId, ADMIN_ID)).thenReturn(job);

        given()
                .contentType(ContentType.JSON)
                .when()
                .post("/api/admin/sms-jobs/" + jobId + "/approve")
                .then()
                .statusCode(200)
                .body("id", is(jobId.toString()))
                .body("message", is("SMS job approved and scheduled for sending"));
    }

    @Test
    @TestSecurity(user = "admin-user", roles = "sys_admin")
    public void testRejectJob() {
        Mockito.when(jwt.getSubject()).thenReturn(ADMIN_ID);

        UUID jobId = UUID.randomUUID();
        SmsJobRejectRequest request = new SmsJobRejectRequest();
        request.setReason("Spam content");

        SmsJob job = new SmsJob();
        job.id = jobId;
        job.approvalStatus = SmsJob.ApprovalStatus.REJECTED;

        Mockito.when(smsJobService.rejectJob(jobId, ADMIN_ID, "Spam content")).thenReturn(job);

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/admin/sms-jobs/" + jobId + "/reject")
                .then()
                .statusCode(200)
                .body("id", is(jobId.toString()))
                .body("message", is("SMS job rejected"));
    }
}
