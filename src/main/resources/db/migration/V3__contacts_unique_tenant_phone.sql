-- Flyway V3: add unique constraint on contacts(tenantId, phone) if missing
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_contacts_tenant_phone'
    ) THEN
        ALTER TABLE contacts ADD CONSTRAINT uk_contacts_tenant_phone UNIQUE (tenantId, phone);
    END IF;
END$$;

