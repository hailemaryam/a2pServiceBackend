package org.hmmk.sms.resource;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import org.hmmk.sms.dto.common.PaginatedResponse;
import org.hmmk.sms.entity.payment.PaymentTransaction;
import org.hmmk.sms.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;

@QuarkusTest
public class PaymentAdminResourceTest {

    @InjectMock
    PaymentService paymentService;

    @Test
    @TestSecurity(user = "sys-admin", roles = "sys_admin")
    public void listAllTransactionsAsAdmin() {
        PaymentTransaction tx = PaymentTransaction.builder()
                .id("trx-1")
                .amountPaid(new BigDecimal("100"))
                .smsCredited(200)
                .paymentStatus(PaymentTransaction.PaymentStatus.SUCCESSFUL)
                .build();

        PaginatedResponse<PaymentTransaction> response = new PaginatedResponse<>(List.of(tx), 1, 0, 20);

        Mockito.when(paymentService.listAllTransactions(eq(0), eq(20), isNull(), isNull())).thenReturn(response);

        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/admin/payments/transactions")
                .then()
                .statusCode(200)
                .body("total", is(1))
                .body("items[0].id", is("trx-1"));
    }
}

