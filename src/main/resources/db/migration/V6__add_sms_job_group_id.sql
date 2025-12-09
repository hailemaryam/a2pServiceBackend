-- Add group_id column to sms_jobs table for GROUP job type
ALTER TABLE sms_jobs ADD COLUMN group_id VARCHAR(255);

COMMENT ON COLUMN sms_jobs.group_id IS 'ContactGroup ID for jobs with JobType.GROUP';
