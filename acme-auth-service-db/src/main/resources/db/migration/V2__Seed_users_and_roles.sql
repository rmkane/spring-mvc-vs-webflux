-- Insert users from LDIF-like structure with ACME_* role names
-- Note: DNs are normalized to lowercase to match LDAP normalization
-- Only input data (given_name, surname) remains un-normalized

-- User 1: John Doe with ACME_READ_WRITE role
INSERT INTO users (dn, given_name, surname, created_at, created_by, updated_at, updated_by)
VALUES ('cn=john doe,ou=engineering,ou=users,dc=corp,dc=acme,dc=org', 'John', 'Doe', NOW(), 'system', NOW(), 'system');

INSERT INTO user_roles (user_id, role_name)
VALUES ((SELECT id FROM users WHERE dn = 'cn=john doe,ou=engineering,ou=users,dc=corp,dc=acme,dc=org'),
        'ACME_READ_WRITE');

-- User 2: Alice Smith with ACME_READ_WRITE role
INSERT INTO users (dn, given_name, surname, created_at, created_by, updated_at, updated_by)
VALUES ('cn=alice smith,ou=hr,ou=users,dc=corp,dc=acme,dc=org', 'Alice', 'Smith', NOW(), 'system', NOW(), 'system');

INSERT INTO user_roles (user_id, role_name)
VALUES ((SELECT id FROM users WHERE dn = 'cn=alice smith,ou=hr,ou=users,dc=corp,dc=acme,dc=org'), 'ACME_READ_WRITE');

-- User 3: Brian Wilson with ACME_READ_ONLY role
INSERT INTO users (dn, given_name, surname, created_at, created_by, updated_at, updated_by)
VALUES ('cn=brian wilson,ou=finance,ou=users,dc=corp,dc=acme,dc=org', 'Brian', 'Wilson', NOW(), 'system', NOW(),
        'system');

INSERT INTO user_roles (user_id, role_name)
VALUES ((SELECT id FROM users WHERE dn = 'cn=brian wilson,ou=finance,ou=users,dc=corp,dc=acme,dc=org'),
        'ACME_READ_ONLY');

-- User 4: Maria Garcia with ACME_READ_ONLY role
INSERT INTO users (dn, given_name, surname, created_at, created_by, updated_at, updated_by)
VALUES ('cn=maria garcia,ou=it,ou=users,dc=corp,dc=acme,dc=org', 'Maria', 'Garcia', NOW(), 'system', NOW(),
        'system');

INSERT INTO user_roles (user_id, role_name)
VALUES ((SELECT id FROM users WHERE dn = 'cn=maria garcia,ou=it,ou=users,dc=corp,dc=acme,dc=org'), 'ACME_READ_ONLY');

-- User 5: Kevin Tran (no roles assigned yet)
INSERT INTO users (dn, given_name, surname, created_at, created_by, updated_at, updated_by)
VALUES ('cn=kevin tran,ou=security,ou=users,dc=corp,dc=acme,dc=org', 'Kevin', 'Tran', NOW(), 'system', NOW(),
        'system');
