-- Clean up existing tables
DROP TABLE IF EXISTS audit_logs CASCADE;
DROP TABLE IF EXISTS issued_certificates CASCADE;
DROP TABLE IF EXISTS certificate_authorities CASCADE;

-- Certificate Authorities Table
CREATE TABLE certificate_authorities (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    hierarchy_level INTEGER NOT NULL,
    label VARCHAR(255) NOT NULL,
    algorithm VARCHAR(50) NOT NULL,
    status VARCHAR(50),
    subject_dn VARCHAR(255),
    organization_id UUID,
    
    -- Crypto material (Text for PEMs, path for keys)
    public_key TEXT,
    certificate TEXT,
    private_key_path TEXT,
    
    valid_from TIMESTAMP,
    valid_until TIMESTAMP,
    
    parent_ca_id UUID REFERENCES certificate_authorities(id)
);

-- Issued Certificates Table
CREATE TABLE issued_certificates (
    id UUID PRIMARY KEY,
    serial_number VARCHAR(255) NOT NULL UNIQUE,
    subject_dn VARCHAR(255) NOT NULL,
    username VARCHAR(255),
    status VARCHAR(50),
    
    certificate TEXT NOT NULL,
    public_key TEXT,
    csr TEXT,
    
    valid_from TIMESTAMP,
    valid_until TIMESTAMP,
    
    revoked_at TIMESTAMP,
    revocation_reason VARCHAR(255),
    
    issuing_ca_id UUID REFERENCES certificate_authorities(id) NOT NULL
);

-- Audit Logs Table
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    actor VARCHAR(255) NOT NULL,
    action VARCHAR(255) NOT NULL,
    target VARCHAR(255),
    details VARCHAR(2048),
    status VARCHAR(50) NOT NULL,
    client_ip VARCHAR(255)
);

-- Indexes for performance
CREATE INDEX idx_ca_parent ON certificate_authorities(parent_ca_id);
CREATE INDEX idx_ca_level ON certificate_authorities(hierarchy_level);
CREATE INDEX idx_cert_issuing_ca ON issued_certificates(issuing_ca_id);
CREATE INDEX idx_cert_serial ON issued_certificates(serial_number);
CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp);
CREATE INDEX idx_audit_actor ON audit_logs(actor);
