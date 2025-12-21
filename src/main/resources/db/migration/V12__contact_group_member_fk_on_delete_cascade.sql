-- Remove orphaned members referencing missing contacts or groups
DELETE FROM contact_group_members
WHERE (contact_id IS NOT NULL AND contact_id NOT IN (SELECT id FROM contacts))
   OR (group_id IS NOT NULL AND group_id NOT IN (SELECT id FROM contact_groups));

-- Drop existing foreign key constraints if they exist (PostgreSQL syntax)
ALTER TABLE contact_group_members
  DROP CONSTRAINT IF EXISTS contact_group_members_contact_id_fkey;

ALTER TABLE contact_group_members
  DROP CONSTRAINT IF EXISTS contact_group_members_group_id_fkey;

-- Ensure columns are NOT NULL to match entity mappings
ALTER TABLE contact_group_members
  ALTER COLUMN contact_id SET NOT NULL;

ALTER TABLE contact_group_members
  ALTER COLUMN group_id SET NOT NULL;

-- Recreate FKs with ON DELETE CASCADE
ALTER TABLE contact_group_members
  ADD CONSTRAINT contact_group_members_contact_id_fkey
    FOREIGN KEY (contact_id) REFERENCES contacts(id) ON DELETE CASCADE;

ALTER TABLE contact_group_members
  ADD CONSTRAINT contact_group_members_group_id_fkey
    FOREIGN KEY (group_id) REFERENCES contact_groups(id) ON DELETE CASCADE;

