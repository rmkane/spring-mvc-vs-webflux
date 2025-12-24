-- Insert 3 users for testing different role scenarios

-- User 1: No roles (user with no permissions)
INSERT INTO users (username, created_at, updated_at) VALUES
('noaccess', NOW(), NOW());

-- User 2: Read-only role
INSERT INTO users (username, created_at, updated_at) VALUES
('readonly', NOW(), NOW());

INSERT INTO user_roles (user_id, role_name) VALUES
((SELECT id FROM users WHERE username = 'readonly'), 'ROLE_READ_ONLY');

-- User 3: Read-write role
INSERT INTO users (username, created_at, updated_at) VALUES
('readwrite', NOW(), NOW());

INSERT INTO user_roles (user_id, role_name) VALUES
((SELECT id FROM users WHERE username = 'readwrite'), 'ROLE_READ_ONLY'),
((SELECT id FROM users WHERE username = 'readwrite'), 'ROLE_READ_WRITE');

