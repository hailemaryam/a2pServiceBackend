-- Add CANCELED to RecipientStatus check constraint
ALTER TABLE sms_recipients DROP CONSTRAINT IF EXISTS sms_recipients_status_check;
ALTER TABLE sms_recipients ADD CONSTRAINT sms_recipients_status_check CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'CANCELED'));
