-- Add email and phone columns, remove domain column from tenant table
ALTER TABLE tenant ADD COLUMN email VARCHAR(255);
ALTER TABLE tenant ADD COLUMN phone VARCHAR(20);
ALTER TABLE tenant DROP COLUMN IF EXISTS domain;
