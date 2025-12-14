ALTER TABLE contacts DROP CONSTRAINT IF EXISTS uk_contacts_tenant_phone;
ALTER TABLE contacts ADD CONSTRAINT uk_contacts_tenant_phone UNIQUE (tenantId, phone);

