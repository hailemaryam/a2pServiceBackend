-- Formatted initial schema migration (made idempotent with IF NOT EXISTS for CREATE TABLE)

CREATE TABLE IF NOT EXISTS contact_group_members (
    id VARCHAR(255) NOT NULL,
    createdAt TIMESTAMP(6) WITH TIME ZONE,
    tenantId VARCHAR(255) NOT NULL,
    updatedAt TIMESTAMP(6) WITH TIME ZONE,
    contact_id VARCHAR(255) NOT NULL,
    group_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS contact_groups (
    id VARCHAR(255) NOT NULL,
    createdAt TIMESTAMP(6) WITH TIME ZONE,
    tenantId VARCHAR(255) NOT NULL,
    updatedAt TIMESTAMP(6) WITH TIME ZONE,
    description VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS contacts (
    id VARCHAR(255) NOT NULL,
    createdAt TIMESTAMP(6) WITH TIME ZONE,
    tenantId VARCHAR(255) NOT NULL,
    updatedAt TIMESTAMP(6) WITH TIME ZONE,
    email VARCHAR(255),
    name VARCHAR(255),
    phone VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS payment_transaction (
    id VARCHAR(255) NOT NULL,
    createdAt TIMESTAMP(6) WITH TIME ZONE,
    tenantId VARCHAR(255) NOT NULL,
    updatedAt TIMESTAMP(6) WITH TIME ZONE,
    amountPaid NUMERIC(38,2) NOT NULL,
    sms_credited INTEGER NOT NULL,
    transaction_type VARCHAR(255) NOT NULL CHECK (transaction_type IN ('TOP_UP','DEDUCTION')),
    sms_package_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS sender (
    id VARCHAR(255) NOT NULL,
    createdAt TIMESTAMP(6) WITH TIME ZONE,
    tenantId VARCHAR(255) NOT NULL,
    updatedAt TIMESTAMP(6) WITH TIME ZONE,
    name VARCHAR(255),
    short_code VARCHAR(200) NOT NULL,
    status VARCHAR(255) NOT NULL CHECK (status IN ('ACTIVE','INACTIVE','PENDING_VERIFICATION','REJECTED')),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS sms_jobs (
    id UUID NOT NULL,
    createdAt TIMESTAMP(6) WITH TIME ZONE,
    tenantId VARCHAR(255) NOT NULL,
    updatedAt TIMESTAMP(6) WITH TIME ZONE,
    approvalStatus VARCHAR(255) NOT NULL CHECK (approvalStatus IN ('PENDING','APPROVED','REJECTED')),
    approvedAt TIMESTAMP(6) WITH TIME ZONE,
    approved_by VARCHAR(255),
    created_by VARCHAR(255) NOT NULL,
    jobType VARCHAR(255) NOT NULL CHECK (jobType IN ('SINGLE','GROUP','BULK')),
    message_content VARCHAR(255) NOT NULL,
    message_type VARCHAR(255) NOT NULL CHECK (message_type IN ('English','UNICODE')),
    scheduledAt TIMESTAMP(6) WITH TIME ZONE,
    senderId VARCHAR(255) NOT NULL,
    sourceType VARCHAR(255) NOT NULL CHECK (sourceType IN ('API','MANUAL','CSV_UPLOAD')),
    status VARCHAR(255) CHECK (status IN ('PENDING_APPROVAL','SCHEDULED','SENDING','COMPLETED','FAILED')),
    totalRecipients BIGINT,
    totalSmsCount BIGINT,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS sms_package_tier (
    id VARCHAR(255) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL,
    max_sms_count INTEGER,
    min_sms_count INTEGER NOT NULL,
    price_per_sms NUMERIC(38,2) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS sms_recipients (
    id UUID NOT NULL,
    createdAt TIMESTAMP(6) WITH TIME ZONE,
    tenantId VARCHAR(255) NOT NULL,
    updatedAt TIMESTAMP(6) WITH TIME ZONE,
    message VARCHAR(255),
    messageType VARCHAR(255) NOT NULL CHECK (messageType IN ('English','UNICODE')),
    phoneNumber VARCHAR(255) NOT NULL,
    senderId VARCHAR(255) NOT NULL,
    sentAt TIMESTAMP(6) WITH TIME ZONE,
    status VARCHAR(255) NOT NULL CHECK (status IN ('PENDING','SENT','FAILED')),
    job_id UUID NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS tenant (
    id VARCHAR(255) NOT NULL,
    config_json TEXT,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    domain VARCHAR(200),
    name VARCHAR(150) NOT NULL,
    sms_credit BIGINT NOT NULL,
    status VARCHAR(255) NOT NULL CHECK (status IN ('ACTIVE','INACTIVE')),
    updated_at TIMESTAMP(6) WITH TIME ZONE,
    PRIMARY KEY (id)
);

ALTER TABLE IF EXISTS sender DROP CONSTRAINT IF EXISTS UKkpvcnj0dbf1ped7343d2wtlf;
ALTER TABLE IF EXISTS sender ADD CONSTRAINT UKkpvcnj0dbf1ped7343d2wtlf UNIQUE (tenantId, short_code);

CREATE INDEX IF NOT EXISTS idx_tenant_job ON sms_recipients (tenantId, job_id);

ALTER TABLE IF EXISTS tenant DROP CONSTRAINT IF EXISTS UKrnffskexgwnnmv6rfrdxujyky;
ALTER TABLE IF EXISTS tenant ADD CONSTRAINT UKrnffskexgwnnmv6rfrdxujyky UNIQUE (domain);

ALTER TABLE IF EXISTS tenant DROP CONSTRAINT IF EXISTS UKdcxf3ksi0gyn1tieeq0id96lm;
ALTER TABLE IF EXISTS tenant ADD CONSTRAINT UKdcxf3ksi0gyn1tieeq0id96lm UNIQUE (name);

ALTER TABLE IF EXISTS contact_group_members ADD CONSTRAINT FK6287o9h8vhwts0i46l82v50xt FOREIGN KEY (contact_id) REFERENCES contacts(id);
ALTER TABLE IF EXISTS contact_group_members ADD CONSTRAINT FKl8f2g7swd3njp8neah7yjo0yh FOREIGN KEY (group_id) REFERENCES contact_groups(id);
ALTER TABLE IF EXISTS payment_transaction ADD CONSTRAINT FKapgh2pmgicnwj3jsfc1r6glb5 FOREIGN KEY (sms_package_id) REFERENCES sms_package_tier(id);
ALTER TABLE IF EXISTS sms_recipients ADD CONSTRAINT FKax2say37ns6b2l2kp76f2hgyt FOREIGN KEY (job_id) REFERENCES sms_jobs(id);
