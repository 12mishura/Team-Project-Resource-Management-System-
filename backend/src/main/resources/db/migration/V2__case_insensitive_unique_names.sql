ALTER TABLE organization DROP CONSTRAINT uk_organization_title;
CREATE UNIQUE INDEX uk_organization_title_lower ON organization (LOWER(title));

ALTER TABLE users DROP CONSTRAINT uk_users_username;
CREATE UNIQUE INDEX uk_users_username_lower ON users (LOWER(username));
