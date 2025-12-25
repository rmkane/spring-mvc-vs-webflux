-- Create users table (LDAP-like structure)
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    dn VARCHAR(500) NOT NULL UNIQUE,
    given_name VARCHAR(100),
    surname VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP,
    updated_by VARCHAR(100)
);

-- Create user_roles junction table for RBAC (many-to-many relationship)
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_name VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role_name),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_users_dn ON users(dn);
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_name ON user_roles(role_name);

