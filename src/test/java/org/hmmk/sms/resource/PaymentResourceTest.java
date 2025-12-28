package org.hmmk.sms.resource;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.hmmk.sms.dto.ChapaCallbackRequest;
import org.hmmk.sms.dto.ChapaInitResponse;
import org.hmmk.sms.dto.ChapaVerifyResponse;
import org.hmmk.sms.dto.PaymentInitRequest;
import org.hmmk.sms.entity.Tenant;
import org.hmmk.sms.entity.payment.PaymentTransaction;
import org.hmmk.sms.entity.payment.SmsPackageTier;
import org.hmmk.sms.service.ChapaPaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.transaction.Transactional;
import java.math.BigDecimal;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@QuarkusTest
public class PaymentResourceTest {

    @InjectMock
    ChapaPaymentService chapaPaymentService;

    @InjectMock
    JsonWebToken jwt;

    private String tenantId;

    @BeforeEach
    @Transactional
    public void setup() {
        // cleanup
        PaymentTransaction.deleteAll();
        SmsPackageTier.deleteAll();
        // Since we are using a dynamic tenant per test run, we don't strictly need to
        // delete all tenants,
        // but it keeps the DB clean.

        // Create a tenant with generated ID
        Tenant tenant = new Tenant();
        tenant.name = "Payment Tenant " + System.currentTimeMillis();
        tenant.phone = "+251911223344";
        tenant.smsCredit = 0;
        tenant.status = Tenant.TenantStatus.ACTIVE;
        tenant.persist();
        this.tenantId = tenant.id;

        // Create a package tier
        SmsPackageTier tier = new SmsPackageTier();
        tier.minSmsCount = 100;
        tier.maxSmsCount = 1000;
        tier.pricePerSms = new BigDecimal("0.50");
        tier.description = "Test Package";
        tier.isActive = true;
        tier.persist();
    }

    @Test
    @TestSecurity(user = "admin", roles = "tenant_admin")
    public void testInitializePayment() {
        Mockito.when(jwt.getClaim("tenantId")).thenReturn(tenantId);

        Mockito.when(chapaPaymentService.initializePayment(anyString(), anyString(), any(), any()))
                .thenReturn(ChapaInitResponse.builder()
                        .status("success")
                        .data(ChapaInitResponse.ChapaData.builder().checkoutUrl("http://chapa.co/pay").build())
                        .build());

        PaymentInitRequest request = new PaymentInitRequest();
        request.setAmount(new BigDecimal("100.00")); // Should buy 200 SMS

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/payments/initialize")
                .then()
                .statusCode(200)
                .body("checkoutUrl", notNullValue())
                .body("transactionId", notNullValue())
                .body("smsCredits", is(200));
    }

    @Test
    public void testHandleCallback() {
        // 1. Create a pending transaction
        Tenant tenant = Tenant.findById(tenantId);
        SmsPackageTier tier = SmsPackageTier.findAll().firstResult();

        String txRef = createPendingTransaction(tenant, tier);

        // 2. Mock Verify
        Mockito.when(chapaPaymentService.verifyPayment(txRef))
                .thenReturn(ChapaVerifyResponse.builder()
                        .status("success")
                        .data(ChapaVerifyResponse.ChapaVerifyData.builder().status("success").build())
                        .build());

        // 3. Call callback
        ChapaCallbackRequest callback = new ChapaCallbackRequest();
        callback.setTrxRef(txRef);
        callback.setStatus("success");

        given()
                .contentType(ContentType.JSON)
                .body(callback)
                .when()
                .post("/api/payments/callback")
                .then()
                .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin", roles = "tenant_admin")
    public void testGetTransactionById() {
        // 1. Create a pending transaction
        Tenant tenant = Tenant.findById(tenantId);
        SmsPackageTier tier = SmsPackageTier.findAll().firstResult();
        String txRef = createPendingTransaction(tenant, tier);

        // 2. Mock JWT
        Mockito.when(jwt.getClaim("tenantId")).thenReturn(tenantId);

        // 3. Mock Chapa Verify (needed because verifyTransaction is called in
        // PaymentResource)
        Mockito.when(chapaPaymentService.verifyPayment(txRef))
                .thenReturn(ChapaVerifyResponse.builder()
                        .status("success")
                        .data(ChapaVerifyResponse.ChapaVerifyData.builder().status("success").build())
                        .build());

        // 4. Call GET and verify it's SUCCESSFUL (200 OK) and serializable
        given()
                .when()
                .get("/api/payments/transactions/{id}", txRef)
                .then()
                .statusCode(200)
                .body("id", is(txRef))
                .body("paymentStatus", is("SUCCESSFUL"))
                .body("smsPackage.id", is(tier.id));
    }

    @Transactional
    String createPendingTransaction(Tenant tenant, SmsPackageTier tier) {
        PaymentTransaction tx = new PaymentTransaction();
        tx.tenantId = tenant.id;
        tx.smsPackage = tier;
        tx.amountPaid = new BigDecimal("100");
        tx.smsCredited = 200;
        tx.paymentStatus = PaymentTransaction.PaymentStatus.IN_PROGRESS;
        tx.persist();
        return tx.id;
    }
}
