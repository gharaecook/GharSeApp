ALTER TABLE payment ADD COLUMN provider_order_id VARCHAR(100);
ALTER TABLE payment ADD COLUMN provider_refund_id VARCHAR(100);
CREATE UNIQUE INDEX uq_payment_provider_order_id ON payment (provider_order_id) WHERE provider_order_id IS NOT NULL;
