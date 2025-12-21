package org.hmmk.sms.resource;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.hmmk.sms.entity.Tenant;
import org.hmmk.sms.entity.contact.Contact;
import org.hmmk.sms.entity.sms.SmsJob;
import org.hmmk.sms.entity.sms.SmsRecipient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.transaction.Transactional;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class DashboardResourceIT {

    @InjectMock
    JsonWebToken jwt;

    private static String TENANT_ID = "";

    @BeforeEach
    @Transactional
    public void setup() {
        // clean up existing data
        SmsRecipient.deleteAll();
        SmsJob.deleteAll();
        Contact.deleteAll();
        Tenant.deleteAll();

        // create tenant
        Tenant tenant = new Tenant();
        tenant.name = "Dashboard Test Tenant";
        tenant.phone = "+10000000000";
        tenant.status = Tenant.TenantStatus.ACTIVE;
        tenant.smsCredit = 250L;
        tenant.persist();
        TENANT_ID = tenant.id;

        // create contacts
        Contact c1 = new Contact();
        c1.tenantId = TENANT_ID;
        c1.phone = "+1111111111";
        c1.name = "Contact One";
        c1.persist();

        Contact c2 = new Contact();
        c2.tenantId = TENANT_ID;
        c2.phone = "+2222222222";
        c2.name = "Contact Two";
        c2.persist();

        // create API job with 2 recipients (SENT)
        SmsJob apiJob = new SmsJob();
        apiJob.tenantId = TENANT_ID;
        apiJob.senderId = "sender-1";
        apiJob.jobType = SmsJob.JobType.BULK;
        apiJob.sourceType = SmsJob.SourceType.API;
        apiJob.messageContent = "Hello API";
        apiJob.messageType = SmsJob.MessageType.English;
        apiJob.totalRecipients = 2L;
        apiJob.totalSmsCount = 2L;
        apiJob.createdBy = "test-user";
        apiJob.approvalStatus = SmsJob.ApprovalStatus.APPROVED;
        apiJob.persist();

        SmsRecipient r1 = new SmsRecipient();
        r1.tenantId = TENANT_ID;
        r1.senderId = "sender-1";
        r1.job = apiJob;
        r1.phoneNumber = "+1111111111";
        r1.message = "Hello 1";
        r1.messageType = SmsJob.MessageType.English;
        r1.status = SmsRecipient.RecipientStatus.SENT;
        r1.persist();

        SmsRecipient r2 = new SmsRecipient();
        r2.tenantId = TENANT_ID;
        r2.senderId = "sender-1";
        r2.job = apiJob;
        r2.phoneNumber = "+2222222222";
        r2.message = "Hello 2";
        r2.messageType = SmsJob.MessageType.English;
        r2.status = SmsRecipient.RecipientStatus.SENT;
        r2.persist();

        // create MANUAL job with 1 recipient (SENT)
        SmsJob manualJob = new SmsJob();
        manualJob.tenantId = TENANT_ID;
        manualJob.senderId = "sender-2";
        manualJob.jobType = SmsJob.JobType.SINGLE;
        manualJob.sourceType = SmsJob.SourceType.MANUAL;
        manualJob.messageContent = "Hello Manual";
        manualJob.messageType = SmsJob.MessageType.English;
        manualJob.totalRecipients = 1L;
        manualJob.totalSmsCount = 1L;
        manualJob.createdBy = "test-user";
        manualJob.approvalStatus = SmsJob.ApprovalStatus.APPROVED;
        manualJob.persist();

        SmsRecipient r3 = new SmsRecipient();
        r3.tenantId = TENANT_ID;
        r3.senderId = "sender-2";
        r3.job = manualJob;
        r3.phoneNumber = "+3333333333";
        r3.message = "Manual Hello";
        r3.messageType = SmsJob.MessageType.English;
        r3.status = SmsRecipient.RecipientStatus.SENT;
        r3.persist();

        // create CSV_UPLOAD job with 0 recipients (to test zero count)
        SmsJob csvJob = new SmsJob();
        csvJob.tenantId = TENANT_ID;
        csvJob.senderId = "sender-3";
        csvJob.jobType = SmsJob.JobType.BULK;
        csvJob.sourceType = SmsJob.SourceType.CSV_UPLOAD;
        csvJob.messageContent = "Hello CSV";
        csvJob.messageType = SmsJob.MessageType.English;
        csvJob.totalRecipients = 0L;
        csvJob.totalSmsCount = 0L;
        csvJob.createdBy = "test-user";
        csvJob.approvalStatus = SmsJob.ApprovalStatus.APPROVED;
        csvJob.persist();
    }

    @Test
    @TestSecurity(user = "tenant-admin", roles = "tenant_admin")
    public void testGetDashboard() {
        Mockito.when(jwt.getClaim("tenantId")).thenReturn(TENANT_ID);

        given()
                .when()
                .get("/api/dashboard")
                .then()
                .statusCode(200)
                .body("remainingCredits", is(250))
                .body("contactCount", is(2))
                .body("smsSentBySource.API", is(2))
                .body("smsSentBySource.MANUAL", is(1))
                .body("smsSentBySource.CSV_UPLOAD", is(0));
    }
}

