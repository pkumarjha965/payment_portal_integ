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
