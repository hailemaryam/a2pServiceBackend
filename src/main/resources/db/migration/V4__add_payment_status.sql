-- Remove transaction_type column and add payment_status column
ALTER TABLE payment_transaction DROP COLUMN IF EXISTS transaction_type;

ALTER TABLE payment_transaction ADD COLUMN payment_status SMALLINT NOT NULL DEFAULT 2;
-- 0 = SUCCESSFUL
-- 1 = FAILED  
-- 2 = IN_PROGRESS (default for new transactions)

COMMENT ON COLUMN payment_transaction.payment_status IS '0=SUCCESSFUL, 1=FAILED, 2=IN_PROGRESS, 3=CANCELED';
