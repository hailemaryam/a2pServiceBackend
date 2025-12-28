-- Add CANCELED to JobStatus check constraint
ALTER TABLE sms_jobs DROP CONSTRAINT IF EXISTS sms_jobs_status_check;
ALTER TABLE sms_jobs ADD CONSTRAINT sms_jobs_status_check CHECK (status IN ('PENDING_APPROVAL', 'SCHEDULED', 'SENDING', 'COMPLETED', 'FAILED', 'CANCELED'));
