-- PostgreSQL DDL Schema (generated based on entities)
-- You might want to fine-tune this, add indexes, constraints etc.

-- Drop tables if they exist (optional, for clean slate during dev)
-- DROP TABLE IF EXISTS tasks CASCADE;
-- DROP TABLE IF EXISTS orders CASCADE;

CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    status VARCHAR(50) NOT NULL,
    created_date TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB
);

CREATE TABLE IF NOT EXISTS tasks (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    task_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_date TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_date TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT fk_task_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

-- Example Indexes (consider adding more based on query patterns)
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_tasks_order_id ON tasks(order_id);
CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status);
CREATE INDEX IF NOT EXISTS idx_tasks_order_id_status ON tasks(order_id, status);

-- Trigger to update 'updated_date' on orders (Optional, Hibernate @UpdateTimestamp handles this at app level)
-- CREATE OR REPLACE FUNCTION update_modified_column()
-- RETURNS TRIGGER AS $$
-- BEGIN
--    NEW.updated_date = CURRENT_TIMESTAMP;
--    RETURN NEW;
-- END;
-- $$ language 'plpgsql';

-- CREATE TRIGGER update_orders_modtime
-- BEFORE UPDATE ON orders
-- FOR EACH ROW
-- EXECUTE PROCEDURE update_modified_column();

-- CREATE TRIGGER update_tasks_modtime
-- BEFORE UPDATE ON tasks
-- FOR EACH ROW
-- EXECUTE PROCEDURE update_modified_column();

```
```json
// Sample JSON for Order Metadata (metadata field in 'orders' table)
{
  "customerInfo": {
    "customerId": "CUST12345",
    "name": "John Doe",
    "email": "john.doe@example.com",
    "phone": "555-123-4567"
  },
  "shippingAddress": {
    "street": "123 Main St",
    "city": "Anytown",
    "state": "CA",
    "zipCode": "90210",
    "country": "USA"
  },
  "billingAddress": {
    "street": "123 Main St",
    "city": "Anytown",
    "state": "CA",
    "zipCode": "90210",
    "country": "USA"
  },
  "items": [
    {
      "productId": "PROD001",
      "productName": "Super Widget",
      "quantity": 2,
      "unitPrice": 25.99
    },
    {
      "productId": "PROD002",
      "productName": "Mega Gadget",
      "quantity": 1,
      "unitPrice": 79.50
    }
  ],
  "paymentDetails": {
    "paymentMethod": "CreditCard",
    "transactionId": "txn_abc123xyz",
    "paymentStatus": "Authorized"
  },
  "deliveryPreferences": {
    "requestedDeliveryDate": "2025-06-15",
    "deliveryInstructions": "Leave package at front door if no one answers."
  },
  "discountApplied": {
    "couponCode": "SUMMER20",
    "discountAmount": 10.00
  },
  "orderNotes": "Customer requested gift wrapping for PROD001."
}

// Another simpler sample
{
  "customerInfo": { "name": "Jane Smith", "email": "jane@example.com" },
  "items": [{ "sku": "SKU001", "qty": 1}]
}
