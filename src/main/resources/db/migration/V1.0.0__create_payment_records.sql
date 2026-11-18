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
CREATE INDEX IF NOT EXISTS idx_payment_records_order_id ON payment_records(order_id);
