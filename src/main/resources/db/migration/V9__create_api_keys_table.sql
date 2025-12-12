CREATE TABLE api_keys (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    sender_id VARCHAR(255) NOT NULL,
    api_key VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    FOREIGN KEY (sender_id) REFERENCES sender(id)
);

CREATE INDEX idx_api_keys_api_key ON api_keys(api_key);
CREATE INDEX idx_api_keys_tenant_id ON api_keys(tenant_id);
