package org.medix;

import org.medix.dto.PaymentStatusResponse;
import org.medix.dto.PaymentValidationResponse;
import org.medix.model.PaymentRecord;
import org.medix.repository.PaymentRepository;

import java.time.Instant;
import java.util.Optional;

public class TestJava {
    public static void main(String[] args) {
        System.out.println("Starting Payment Verification Tests...");

        PaymentRepository repository = new PaymentRepository();

        // 1. Test Save & Find
        PaymentRecord record = new PaymentRecord(
                "cs_test_abc123",
                "cs_test_abc123",
                "pi_test_xyz789",
                "INV-1001",
                "PENDING",
                10000L,
                "usd",
                "customer@test.com",
                "card",
                Instant.now(),
                Instant.now()
        );
        repository.saveOrUpdate(record);

        Optional<PaymentRecord> found = repository.findByPaymentId("cs_test_abc123");
        if (found.isEmpty() || !"PENDING".equals(found.get().getStatus())) {
            throw new AssertionError("Test Failed: Record not found or status not PENDING");
        }
        System.out.println("✓ Test 1 Passed: Payment saved and retrieved by sessionId");

        // 2. Test Find by payment_intent_id
        Optional<PaymentRecord> foundByPi = repository.findByPaymentId("pi_test_xyz789");
        if (foundByPi.isEmpty() || !"cs_test_abc123".equals(foundByPi.get().getId())) {
            throw new AssertionError("Test Failed: Record not found by payment_intent_id");
        }
        System.out.println("✓ Test 2 Passed: Payment retrieved by payment_intent_id");

        // 3. Test Status Update (simulate webhook)
        repository.updateStatus("cs_test_abc123", "PAID");
        Optional<PaymentRecord> updated = repository.findByPaymentId("cs_test_abc123");
        if (updated.isEmpty() || !"PAID".equals(updated.get().getStatus())) {
            throw new AssertionError("Test Failed: Record status not updated to PAID");
        }
        System.out.println("✓ Test 3 Passed: Payment status updated to PAID");

        // 4. Test DTOs
        PaymentStatusResponse statusResponse = new PaymentStatusResponse(
                updated.get().getId(),
                updated.get().getSessionId(),
                updated.get().getPaymentIntentId(),
                updated.get().getOrderId(),
                updated.get().getStatus(),
                updated.get().getAmount(),
                updated.get().getCurrency(),
                updated.get().getCustomerEmail(),
                updated.get().getPaymentMethod(),
                updated.get().getUpdatedAt().toString()
        );
        if (!"PAID".equals(statusResponse.status()) || statusResponse.amount() != 10000L) {
            throw new AssertionError("Test Failed: PaymentStatusResponse mismatch");
        }
        System.out.println("✓ Test 4 Passed: PaymentStatusResponse generated correctly");

        // 5. Test Validation Response
        PaymentValidationResponse valResponse = new PaymentValidationResponse(
                statusResponse.paymentId(),
                statusResponse.status(),
                true,
                "Payment validated successfully from database records",
                statusResponse.amount(),
                statusResponse.currency(),
                statusResponse.customerEmail()
        );
        if (!valResponse.isValid() || !"PAID".equals(valResponse.status())) {
            throw new AssertionError("Test Failed: PaymentValidationResponse mismatch");
        }
        System.out.println("✓ Test 5 Passed: PaymentValidationResponse confirmed valid");

        System.out.println("\nAll Payment Verification Tests Passed Successfully!");
    }
}
