-- Create enum type for order status if not exists
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'order_status') THEN
        CREATE TYPE order_status AS ENUM ('CREATED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED');
    END IF;
END $$;

-- Insert sample orders with detailed metadata
INSERT INTO orders (status, created_date, updated_date, metadata)
VALUES 
    ('CREATED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 
    '{
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
        "items": [
            {
                "productId": "PROD001",
                "productName": "Super Widget",
                "quantity": 2,
                "unitPrice": 25.99
            }
        ],
        "paymentDetails": {
            "paymentMethod": "CreditCard",
            "transactionId": "txn_abc123xyz",
            "paymentStatus": "Pending"
        }
    }'::jsonb),
    
    ('IN_PROGRESS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 
    '{
        "customerInfo": {
            "customerId": "CUST12346",
            "name": "Jane Smith",
            "email": "jane@example.com"
        },
        "items": [
            {
                "sku": "SKU001",
                "qty": 1
            }
        ],
        "deliveryPreferences": {
            "requestedDeliveryDate": "2025-06-15",
            "deliveryInstructions": "Leave package at front door"
        }
    }'::jsonb),
    
    ('COMPLETED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 
    '{
        "customerInfo": {
            "customerId": "CUST12347",
            "name": "Bob Johnson",
            "email": "bob@example.com",
            "phone": "555-999-8888"
        },
        "items": [
            {
                "productId": "PROD002",
                "productName": "Mega Gadget",
                "quantity": 1,
                "unitPrice": 79.50
            }
        ],
        "discountApplied": {
            "couponCode": "SUMMER20",
            "discountAmount": 10.00
        }
    }'::jsonb);

-- Insert sample tasks for each order with various task types
INSERT INTO tasks (task_type, status, created_date, updated_date, completed_date, order_id)
SELECT 
    task_type,
    status,
    created_date,
    updated_date,
    completed_date,
    order_id
FROM (
    -- For first order (CREATED)
    SELECT 'VALIDATE_ORDER_DETAILS' as task_type, 'COMPLETED' as status, 
           CURRENT_TIMESTAMP - interval '1 hour' as created_date,
           CURRENT_TIMESTAMP as updated_date,
           CURRENT_TIMESTAMP as completed_date,
           o.id as order_id
    FROM orders o WHERE status = 'CREATED'
    UNION ALL
    SELECT 'PROCESS_PAYMENT', 'PENDING', 
           CURRENT_TIMESTAMP - interval '30 minutes',
           CURRENT_TIMESTAMP,
           NULL,
           o.id
    FROM orders o WHERE status = 'CREATED'
    
    UNION ALL
    
    -- For second order (IN_PROGRESS)
    SELECT 'VALIDATE_ORDER_DETAILS', 'COMPLETED',
           CURRENT_TIMESTAMP - interval '2 hours',
           CURRENT_TIMESTAMP - interval '1 hour',
           CURRENT_TIMESTAMP - interval '1 hour',
           o.id
    FROM orders o WHERE status = 'IN_PROGRESS'
    UNION ALL
    SELECT 'PROCESS_PAYMENT', 'COMPLETED',
           CURRENT_TIMESTAMP - interval '1 hour',
           CURRENT_TIMESTAMP - interval '30 minutes',
           CURRENT_TIMESTAMP - interval '30 minutes',
           o.id
    FROM orders o WHERE status = 'IN_PROGRESS'
    UNION ALL
    SELECT 'CHECK_INVENTORY', 'IN_PROGRESS',
           CURRENT_TIMESTAMP - interval '30 minutes',
           CURRENT_TIMESTAMP,
           NULL,
           o.id
    FROM orders o WHERE status = 'IN_PROGRESS'
    
    UNION ALL
    
    -- For third order (COMPLETED)
    SELECT task_type, 'COMPLETED',
           CURRENT_TIMESTAMP - interval '1 day',
           CURRENT_TIMESTAMP - interval '12 hours',
           CURRENT_TIMESTAMP - interval '12 hours',
           o.id
    FROM orders o 
    CROSS JOIN (
        VALUES 
            ('VALIDATE_ORDER_DETAILS'),
            ('PROCESS_PAYMENT'),
            ('CHECK_INVENTORY'),
            ('PACKAGING'),
            ('ARRANGE_SHIPPING')
    ) t(task_type)
    WHERE o.status = 'COMPLETED'
) task_data; 