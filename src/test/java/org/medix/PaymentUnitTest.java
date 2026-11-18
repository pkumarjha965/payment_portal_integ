package org.medix;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.medix.dto.PaymentStatusResponse;
import org.medix.dto.PaymentValidationResponse;
import org.medix.model.PaymentRecord;
import org.medix.repository.PaymentRepository;

import java.time.Instant;
import java.util.Optional;

public class PaymentUnitTest {

    private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        paymentRepository = new PaymentRepository();
    }

    @Test
    void testPaymentRecordCreationAndGetters() {
        Instant now = Instant.now();
        PaymentRecord record = new PaymentRecord(
                "cs_test_123",
                "cs_test_123",
                "pi_test_456",
                "INV-1001",
                "PENDING",
                10000L,
                "usd",
                "customer@example.com",
                "card",
                now,
                now
        );

        Assertions.assertEquals("cs_test_123", record.getId());
        Assertions.assertEquals("cs_test_123", record.getSessionId());
        Assertions.assertEquals("pi_test_456", record.getPaymentIntentId());
        Assertions.assertEquals("INV-1001", record.getOrderId());
        Assertions.assertEquals("PENDING", record.getStatus());
        Assertions.assertEquals(10000L, record.getAmount());
        Assertions.assertEquals("usd", record.getCurrency());
        Assertions.assertEquals("customer@example.com", record.getCustomerEmail());
        Assertions.assertEquals("card", record.getPaymentMethod());
    }

    @Test
    void testRepositorySaveAndFindById() {
        PaymentRecord record = new PaymentRecord(
                "cs_test_abc",
                "cs_test_abc",
                "pi_test_xyz",
                "INV-2002",
                "PENDING",
                5000L,
                "usd",
                "user@test.com",
                "card",
                Instant.now(),
                Instant.now()
        );

        paymentRepository.saveOrUpdate(record);

        // Find by primary ID
        Optional<PaymentRecord> foundById = paymentRepository.findByPaymentId("cs_test_abc");
        Assertions.assertTrue(foundById.isPresent());
        Assertions.assertEquals("PENDING", foundById.get().getStatus());
        Assertions.assertEquals(5000L, foundById.get().getAmount());

        // Find by PaymentIntent ID
        Optional<PaymentRecord> foundByPi = paymentRepository.findByPaymentId("pi_test_xyz");
        Assertions.assertTrue(foundByPi.isPresent());
        Assertions.assertEquals("cs_test_abc", foundByPi.get().getId());
    }

    @Test
    void testRepositoryStatusUpdate() {
        PaymentRecord record = new PaymentRecord(
                "cs_test_upd",
                "cs_test_upd",
                "pi_test_upd",
                "INV-3003",
                "PENDING",
                7500L,
                "usd",
                "payer@test.com",
                "card",
                Instant.now(),
                Instant.now()
        );

        paymentRepository.saveOrUpdate(record);
        paymentRepository.updateStatus("cs_test_upd", "PAID");

        Optional<PaymentRecord> updated = paymentRepository.findByPaymentId("cs_test_upd");
        Assertions.assertTrue(updated.isPresent());
        Assertions.assertEquals("PAID", updated.get().getStatus());
    }

    @Test
    void testPaymentStatusResponse() {
        PaymentStatusResponse response = new PaymentStatusResponse(
                "cs_test_123",
                "cs_test_123",
                "pi_test_456",
                "INV-1001",
                "PAID",
                10000L,
                "usd",
                "client@example.com",
                "card",
                Instant.now().toString()
        );

        Assertions.assertEquals("cs_test_123", response.paymentId());
        Assertions.assertEquals("PAID", response.status());
        Assertions.assertEquals(10000L, response.amount());
    }

    @Test
    void testPaymentValidationResponse() {
        PaymentValidationResponse response = new PaymentValidationResponse(
                "cs_test_123",
                "PAID",
                true,
                "Payment validated successfully from database records",
                10000L,
                "usd",
                "client@example.com"
        );

        Assertions.assertEquals("cs_test_123", response.paymentId());
        Assertions.assertEquals("PAID", response.status());
        Assertions.assertTrue(response.isValid());
        Assertions.assertEquals("Payment validated successfully from database records", response.message());
    }
}
