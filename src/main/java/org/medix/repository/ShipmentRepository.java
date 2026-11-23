package org.medix.repository;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.medix.model.ShipmentRecord;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ShipmentRepository {

    private static final Logger LOG = Logger.getLogger(ShipmentRepository.class);

    @Inject
    DataSource dataSource;

    // Resilient in-memory fallback for local development without PostgreSQL
    private final Map<String, ShipmentRecord> idStore = new ConcurrentHashMap<>();
    private final Map<String, ShipmentRecord> trackingStore = new ConcurrentHashMap<>();
    private final Map<String, ShipmentRecord> orderStore = new ConcurrentHashMap<>();
    private final Map<String, ShipmentRecord> paymentStore = new ConcurrentHashMap<>();

    @PostConstruct
    void onStart() {
        initTable();
    }

    public void initTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS shipment_records (
                id VARCHAR(255) PRIMARY KEY,
                order_id VARCHAR(255),
                payment_id VARCHAR(255),
                tracking_number VARCHAR(100) UNIQUE NOT NULL,
                carrier VARCHAR(100) NOT NULL,
                shipping_method VARCHAR(50) NOT NULL,
                status VARCHAR(50) NOT NULL,
                recipient_name VARCHAR(255),
                street VARCHAR(255),
                city VARCHAR(100),
                state VARCHAR(100),
                postal_code VARCHAR(50),
                country VARCHAR(100),
                label_url VARCHAR(500),
                estimated_delivery TIMESTAMP WITH TIME ZONE,
                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
            );
            CREATE INDEX IF NOT EXISTS idx_shipments_order_id ON shipment_records(order_id);
            CREATE INDEX IF NOT EXISTS idx_shipments_payment_id ON shipment_records(payment_id);
            CREATE INDEX IF NOT EXISTS idx_shipments_tracking_number ON shipment_records(tracking_number);
            """;
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            LOG.info("ShipmentRepository: shipment_records table verified/created in PostgreSQL.");
        } catch (Exception e) {
            LOG.warn("ShipmentRepository: Could not initialize PostgreSQL schema (" + e.getMessage() + "). Operating with in-memory fallback.");
        }
    }

    public ShipmentRecord saveOrUpdate(ShipmentRecord record) {
        if (record.getId() != null) {
            idStore.put(record.getId(), record);
        }
        if (record.getTrackingNumber() != null) {
            trackingStore.put(record.getTrackingNumber(), record);
        }
        if (record.getOrderId() != null) {
            orderStore.put(record.getOrderId(), record);
        }
        if (record.getPaymentId() != null) {
            paymentStore.put(record.getPaymentId(), record);
        }

        String sql = """
            INSERT INTO shipment_records (
                id, order_id, payment_id, tracking_number, carrier, shipping_method,
                status, recipient_name, street, city, state, postal_code, country,
                label_url, estimated_delivery, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                status = EXCLUDED.status,
                label_url = COALESCE(EXCLUDED.label_url, shipment_records.label_url),
                estimated_delivery = COALESCE(EXCLUDED.estimated_delivery, shipment_records.estimated_delivery),
                updated_at = EXCLUDED.updated_at;
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, record.getId());
            ps.setString(2, record.getOrderId());
            ps.setString(3, record.getPaymentId());
            ps.setString(4, record.getTrackingNumber());
            ps.setString(5, record.getCarrier());
            ps.setString(6, record.getShippingMethod());
            ps.setString(7, record.getStatus());
            ps.setString(8, record.getRecipientName());
            ps.setString(9, record.getStreet());
            ps.setString(10, record.getCity());
            ps.setString(11, record.getState());
            ps.setString(12, record.getPostalCode());
            ps.setString(13, record.getCountry());
            ps.setString(14, record.getLabelUrl());
            ps.setTimestamp(15, record.getEstimatedDelivery() != null ? Timestamp.from(record.getEstimatedDelivery()) : null);
            ps.setTimestamp(16, record.getCreatedAt() != null ? Timestamp.from(record.getCreatedAt()) : Timestamp.from(Instant.now()));
            ps.setTimestamp(17, Timestamp.from(Instant.now()));

            ps.executeUpdate();
        } catch (Exception e) {
            LOG.warnf("ShipmentRepository: Failed to save to PostgreSQL (%s). In-memory cache is active.", e.getMessage());
        }

        return record;
    }

    public Optional<ShipmentRecord> findByTrackingNumber(String trackingNumber) {
        if (trackingNumber == null) return Optional.empty();

        String sql = "SELECT * FROM shipment_records WHERE tracking_number = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, trackingNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (Exception e) {
            LOG.debugf("ShipmentRepository query exception: %s. Checking in-memory store.", e.getMessage());
        }

        return Optional.ofNullable(trackingStore.get(trackingNumber));
    }

    public Optional<ShipmentRecord> findByOrderId(String orderId) {
        if (orderId == null) return Optional.empty();

        String sql = "SELECT * FROM shipment_records WHERE order_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (Exception e) {
            LOG.debugf("ShipmentRepository query exception: %s. Checking in-memory store.", e.getMessage());
        }

        return Optional.ofNullable(orderStore.get(orderId));
    }

    public Optional<ShipmentRecord> findByPaymentId(String paymentId) {
        if (paymentId == null) return Optional.empty();

        String sql = "SELECT * FROM shipment_records WHERE payment_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, paymentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (Exception e) {
            LOG.debugf("ShipmentRepository query exception: %s. Checking in-memory store.", e.getMessage());
        }

        return Optional.ofNullable(paymentStore.get(paymentId));
    }

    public boolean updateStatus(String trackingNumber, String newStatus) {
        ShipmentRecord mem = trackingStore.get(trackingNumber);
        if (mem != null) {
            mem.setStatus(newStatus);
            mem.setUpdatedAt(Instant.now());
        }

        String sql = "UPDATE shipment_records SET status = ?, updated_at = ? WHERE tracking_number = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setTimestamp(2, Timestamp.from(Instant.now()));
            ps.setString(3, trackingNumber);
            int rows = ps.executeUpdate();
            return rows > 0 || mem != null;
        } catch (Exception e) {
            LOG.warnf("ShipmentRepository updateStatus exception: %s", e.getMessage());
            return mem != null;
        }
    }

    private ShipmentRecord mapRow(ResultSet rs) throws SQLException {
        Timestamp estDeliveryTs = rs.getTimestamp("estimated_delivery");
        Timestamp createdTs = rs.getTimestamp("created_at");
        Timestamp updatedTs = rs.getTimestamp("updated_at");

        return new ShipmentRecord(
                rs.getString("id"),
                rs.getString("order_id"),
                rs.getString("payment_id"),
                rs.getString("tracking_number"),
                rs.getString("carrier"),
                rs.getString("shipping_method"),
                rs.getString("status"),
                rs.getString("recipient_name"),
                rs.getString("street"),
                rs.getString("city"),
                rs.getString("state"),
                rs.getString("postal_code"),
                rs.getString("country"),
                rs.getString("label_url"),
                estDeliveryTs != null ? estDeliveryTs.toInstant() : null,
                createdTs != null ? createdTs.toInstant() : Instant.now(),
                updatedTs != null ? updatedTs.toInstant() : Instant.now()
        );
    }
}
