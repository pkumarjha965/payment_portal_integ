package org.medix.repository;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.medix.model.PaymentRecord;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class PaymentRepository {

    private static final Logger LOG = Logger.getLogger(PaymentRepository.class);

    @Inject
    DataSource dataSource;

    // Resilient in-memory fallback for local development/testing without active PostgreSQL
    private final Map<String, PaymentRecord> memoryStore = new ConcurrentHashMap<>();

    @PostConstruct
    void onStart() {
        initTable();
    }

    public void initTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS payment_records (
                id VARCHAR(255) PRIMARY KEY,
                session_id VARCHAR(255),
                payment_intent_id VARCHAR(255),
                order_id VARCHAR(255),
                status VARCHAR(50) NOT NULL,
                amount BIGINT,
                currency VARCHAR(10),
                customer_email VARCHAR(255),
                payment_method VARCHAR(50),
                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
            );
            CREATE INDEX IF NOT EXISTS idx_payment_records_session_id ON payment_records(session_id);
            CREATE INDEX IF NOT EXISTS idx_payment_records_payment_intent_id ON payment_records(payment_intent_id);
            """;
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            LOG.info("PaymentRepository: payment_records table verified/created in PostgreSQL.");
        } catch (Exception e) {
            LOG.warn("PaymentRepository: Could not initialize PostgreSQL schema (" + e.getMessage() + "). Operating with in-memory fallback.");
        }
    }

    public PaymentRecord saveOrUpdate(PaymentRecord record) {
        // Cache in memoryStore
        if (record.getId() != null) {
            memoryStore.put(record.getId(), record);
        }
        if (record.getSessionId() != null) {
            memoryStore.put(record.getSessionId(), record);
        }
        if (record.getPaymentIntentId() != null) {
            memoryStore.put(record.getPaymentIntentId(), record);
        }

        String sql = """
            INSERT INTO payment_records (
                id, session_id, payment_intent_id, order_id, status, amount, currency, customer_email, payment_method, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                session_id = COALESCE(EXCLUDED.session_id, payment_records.session_id),
                payment_intent_id = COALESCE(EXCLUDED.payment_intent_id, payment_records.payment_intent_id),
                order_id = COALESCE(EXCLUDED.order_id, payment_records.order_id),
                status = EXCLUDED.status,
                amount = COALESCE(EXCLUDED.amount, payment_records.amount),
                currency = COALESCE(EXCLUDED.currency, payment_records.currency),
                customer_email = COALESCE(EXCLUDED.customer_email, payment_records.customer_email),
                payment_method = COALESCE(EXCLUDED.payment_method, payment_records.payment_method),
                updated_at = EXCLUDED.updated_at;
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, record.getId());
            ps.setString(2, record.getSessionId());
            ps.setString(3, record.getPaymentIntentId());
            ps.setString(4, record.getOrderId());
            ps.setString(5, record.getStatus());
            if (record.getAmount() != null) {
                ps.setLong(6, record.getAmount());
            } else {
                ps.setNull(6, Types.BIGINT);
            }
            ps.setString(7, record.getCurrency());
            ps.setString(8, record.getCustomerEmail());
            ps.setString(9, record.getPaymentMethod());
            ps.setTimestamp(10, Timestamp.from(record.getCreatedAt() != null ? record.getCreatedAt() : Instant.now()));
            ps.setTimestamp(11, Timestamp.from(record.getUpdatedAt() != null ? record.getUpdatedAt() : Instant.now()));
            ps.executeUpdate();
            LOG.infof("Payment record [%s] persisted to DB with status: %s", record.getId(), record.getStatus());
        } catch (Exception e) {
            LOG.warnf("Failed to persist to PostgreSQL (%s). Saved to in-memory store.", e.getMessage());
        }

        return record;
    }

    public Optional<PaymentRecord> findByPaymentId(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }

        String sql = """
            SELECT id, session_id, payment_intent_id, order_id, status, amount, currency, customer_email, payment_method, created_at, updated_at
            FROM payment_records
            WHERE id = ? OR session_id = ? OR payment_intent_id = ?
            ORDER BY updated_at DESC LIMIT 1;
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, identifier);
            ps.setString(2, identifier);
            ps.setString(3, identifier);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PaymentRecord r = mapRow(rs);
                    // update cache
                    memoryStore.put(r.getId(), r);
                    if (r.getSessionId() != null) memoryStore.put(r.getSessionId(), r);
                    if (r.getPaymentIntentId() != null) memoryStore.put(r.getPaymentIntentId(), r);
                    return Optional.of(r);
                }
            }
        } catch (Exception e) {
            LOG.warnf("Failed to query DB for [%s] (%s). Checking in-memory store.", identifier, e.getMessage());
        }

        return Optional.ofNullable(memoryStore.get(identifier));
    }

    public boolean updateStatus(String identifier, String newStatus) {
        if (identifier == null || identifier.isBlank()) {
            return false;
        }

        PaymentRecord memoryRecord = memoryStore.get(identifier);
        if (memoryRecord != null) {
            memoryRecord.setStatus(newStatus);
            memoryRecord.setUpdatedAt(Instant.now());
        }

        String sql = """
            UPDATE payment_records
            SET status = ?, updated_at = ?
            WHERE id = ? OR session_id = ? OR payment_intent_id = ?;
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setTimestamp(2, Timestamp.from(Instant.now()));
            ps.setString(3, identifier);
            ps.setString(4, identifier);
            ps.setString(5, identifier);
            int rows = ps.executeUpdate();
            return rows > 0 || memoryRecord != null;
        } catch (Exception e) {
            LOG.warnf("Failed to update status in DB for [%s] (%s).", identifier, e.getMessage());
            return memoryRecord != null;
        }
    }

    private PaymentRecord mapRow(ResultSet rs) throws SQLException {
        Timestamp createdTs = rs.getTimestamp("created_at");
        Timestamp updatedTs = rs.getTimestamp("updated_at");
        return new PaymentRecord(
                rs.getString("id"),
                rs.getString("session_id"),
                rs.getString("payment_intent_id"),
                rs.getString("order_id"),
                rs.getString("status"),
                rs.getObject("amount") != null ? rs.getLong("amount") : null,
                rs.getString("currency"),
                rs.getString("customer_email"),
                rs.getString("payment_method"),
                createdTs != null ? createdTs.toInstant() : Instant.now(),
                updatedTs != null ? updatedTs.toInstant() : Instant.now()
        );
    }
}
