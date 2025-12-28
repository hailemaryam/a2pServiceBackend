-- Add company information fields to tenant table
ALTER TABLE tenant ADD COLUMN is_company BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tenant ADD COLUMN tin_number VARCHAR(30);
ALTER TABLE tenant ADD COLUMN description VARCHAR(1000);
