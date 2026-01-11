-- Insert users from LDIF-like structure with ACME_* role names
-- Note: Subject DNs are normalized to lowercase to match LDAP normalization
-- Only input data (given_name, surname) remains un-normalized
-- All certificates are issued by: CN=Acme Intermediate CA,O=Acme Corp,C=US

-- User 1: John Doe with ACME_READ_WRITE role
-- CN in Subject DN is the UID (jdoe) to match certificate Subject DN format
INSERT INTO users (subject_dn, issuer_dn, given_name, surname, created_at, created_by, updated_at, updated_by)
VALUES ('cn=jdoe,ou=engineering,ou=users,dc=corp,dc=acme,dc=org', 'CN=Acme Intermediate CA,O=Acme Corp,C=US', 'John', 'Doe', NOW(), 'system', NOW(), 'system');

INSERT INTO user_roles (user_id, role_name)
VALUES ((SELECT id FROM users WHERE subject_dn = 'cn=jdoe,ou=engineering,ou=users,dc=corp,dc=acme,dc=org'),
        'ACME_READ_WRITE');

-- User 2: Alice Smith with ACME_READ_WRITE role
INSERT INTO users (subject_dn, issuer_dn, given_name, surname, created_at, created_by, updated_at, updated_by)
VALUES ('cn=asmith,ou=hr,ou=users,dc=corp,dc=acme,dc=org', 'CN=Acme Intermediate CA,O=Acme Corp,C=US', 'Alice', 'Smith', NOW(), 'system', NOW(), 'system');

INSERT INTO user_roles (user_id, role_name)
VALUES ((SELECT id FROM users WHERE subject_dn = 'cn=asmith,ou=hr,ou=users,dc=corp,dc=acme,dc=org'), 'ACME_READ_WRITE');

-- User 3: Brian Wilson with ACME_READ_ONLY role
INSERT INTO users (subject_dn, issuer_dn, given_name, surname, created_at, created_by, updated_at, updated_by)
VALUES ('cn=bwilson,ou=finance,ou=users,dc=corp,dc=acme,dc=org', 'CN=Acme Intermediate CA,O=Acme Corp,C=US', 'Brian', 'Wilson', NOW(), 'system', NOW(),
        'system');

INSERT INTO user_roles (user_id, role_name)
VALUES ((SELECT id FROM users WHERE subject_dn = 'cn=bwilson,ou=finance,ou=users,dc=corp,dc=acme,dc=org'),
        'ACME_READ_ONLY');

-- User 4: Maria Garcia with ACME_READ_ONLY role
INSERT INTO users (subject_dn, issuer_dn, given_name, surname, created_at, created_by, updated_at, updated_by)
VALUES ('cn=mgarcia,ou=it,ou=users,dc=corp,dc=acme,dc=org', 'CN=Acme Intermediate CA,O=Acme Corp,C=US', 'Maria', 'Garcia', NOW(), 'system', NOW(),
        'system');

INSERT INTO user_roles (user_id, role_name)
VALUES ((SELECT id FROM users WHERE subject_dn = 'cn=mgarcia,ou=it,ou=users,dc=corp,dc=acme,dc=org'), 'ACME_READ_ONLY');

-- User 5: Kevin Tran (no roles assigned yet)
INSERT INTO users (subject_dn, issuer_dn, given_name, surname, created_at, created_by, updated_at, updated_by)
VALUES ('cn=ktran,ou=security,ou=users,dc=corp,dc=acme,dc=org', 'CN=Acme Intermediate CA,O=Acme Corp,C=US', 'Kevin', 'Tran', NOW(), 'system', NOW(),
        'system');
