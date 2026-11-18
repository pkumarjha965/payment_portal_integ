package org.medix;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.medix.model.PaymentRecord;
import org.medix.repository.PaymentRepository;

import java.time.Instant;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class PaymentResourceTest {

    @Inject
    PaymentRepository paymentRepository;

    @BeforeEach
    void setupTestData() {
        PaymentRecord record = new PaymentRecord(
                "cs_test_mock123",
                "cs_test_mock123",
                "pi_test_mock456",
                "INV-1001",
                "PAID",
                10000L,
                "usd",
                "customer@example.com",
                "card",
                Instant.now(),
                Instant.now()
        );
        paymentRepository.saveOrUpdate(record);
    }

    @Test
    void testGetPaymentStatusFound() {
        given()
                .when().get("/api/payments/status/cs_test_mock123")
                .then()
                .statusCode(200)
                .body("status", is("PAID"))
                .body("amount", is(10000))
                .body("currency", is("usd"))
                .body("orderId", is("INV-1001"));
    }

    @Test
    void testGetPaymentStatusNotFound() {
        given()
                .when().get("/api/payments/status/non_existing_id")
                .then()
                .statusCode(404);
    }

    @Test
    void testValidatePaymentPaid() {
        given()
                .when().get("/api/payments/validate/cs_test_mock123")
                .then()
                .statusCode(200)
                .body("status", is("PAID"))
                .body("isValid", is(true))
                .body("amount", is(10000));
    }
}
