-- Insert users from USERS.md (LDAP-like structure)

-- User 1: John Doe with READ_WRITE role
INSERT INTO users (dn, given_name, surname, created_at, created_by, updated_at, updated_by)
VALUES ('cn=John Doe,ou=Engineering,ou=Users,dc=corp,dc=acme,dc=org', 'John', 'Doe', NOW(), 'system', NOW(), 'system');

INSERT INTO user_roles (user_id, role_name)
VALUES ((SELECT id FROM users WHERE dn = 'cn=John Doe,ou=Engineering,ou=Users,dc=corp,dc=acme,dc=org'),
        'ROLE_READ_WRITE');

-- User 2: Alice Smith with READ_WRITE role
INSERT INTO users (dn, given_name, surname, created_at, created_by, updated_at, updated_by)
VALUES ('cn=Alice Smith,ou=HR,ou=Users,dc=corp,dc=acme,dc=org', 'Alice', 'Smith', NOW(), 'system', NOW(), 'system');

INSERT INTO user_roles (user_id, role_name)
VALUES ((SELECT id FROM users WHERE dn = 'cn=Alice Smith,ou=HR,ou=Users,dc=corp,dc=acme,dc=org'), 'ROLE_READ_WRITE');

-- User 3: Brian Wilson with READ_ONLY role
INSERT INTO users (dn, given_name, surname, created_at, created_by, updated_at, updated_by)
VALUES ('cn=Brian Wilson,ou=Finance,ou=Users,dc=corp,dc=acme,dc=org', 'Brian', 'Wilson', NOW(), 'system', NOW(),
        'system');

INSERT INTO user_roles (user_id, role_name)
VALUES ((SELECT id FROM users WHERE dn = 'cn=Brian Wilson,ou=Finance,ou=Users,dc=corp,dc=acme,dc=org'),
        'ROLE_READ_ONLY');

-- User 4: Maria Garcia with READ_ONLY role
INSERT INTO users (dn, given_name, surname, created_at, created_by, updated_at, updated_by)
VALUES ('cn=Maria Garcia,ou=IT,ou=Users,dc=corp,dc=example,dc=com', 'Maria', 'Garcia', NOW(), 'system', NOW(),
        'system');

INSERT INTO user_roles (user_id, role_name)
VALUES ((SELECT id FROM users WHERE dn = 'cn=Maria Garcia,ou=IT,ou=Users,dc=corp,dc=example,dc=com'), 'ROLE_READ_ONLY');

-- User 5: Kevin Tran (assign roles as needed)
INSERT INTO users (dn, given_name, surname, created_at, created_by, updated_at, updated_by)
VALUES ('cn=Kevin Tran,ou=Security,ou=Users,dc=corp,dc=example,dc=com', 'Kevin', 'Tran', NOW(), 'system', NOW(),
        'system');

