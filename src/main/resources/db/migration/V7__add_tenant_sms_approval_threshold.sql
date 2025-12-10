-- Add sms_approval_threshold column to tenant table
-- This defines the minimum number of recipients that triggers admin approval for SMS jobs
ALTER TABLE tenant ADD COLUMN sms_approval_threshold INTEGER NOT NULL DEFAULT 100;

COMMENT ON COLUMN tenant.sms_approval_threshold IS 'Number of recipients above which an SMS job requires admin approval';
