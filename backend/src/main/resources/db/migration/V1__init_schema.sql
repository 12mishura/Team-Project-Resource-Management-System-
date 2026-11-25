CREATE TABLE users
(
    id            UUID                        NOT NULL,
    email         VARCHAR(255)                NOT NULL,
    username      VARCHAR(50)                 NOT NULL,
    password_hash VARCHAR(255)                NOT NULL,
    created_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_username UNIQUE (username)
);

CREATE TABLE organization
(
    id          UUID                        NOT NULL,
    title       VARCHAR(50)                 NOT NULL,
    description VARCHAR(400),
    invite_code VARCHAR(9)                  NOT NULL,
    created_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_organization PRIMARY KEY (id),
    CONSTRAINT uk_organization_title UNIQUE (title),
    CONSTRAINT uk_organization_invite_code UNIQUE (invite_code)
);

CREATE TABLE organization_members
(
    id              UUID                        NOT NULL,
    organization_id UUID                        NOT NULL,
    user_id         UUID                        NOT NULL,
    role            VARCHAR(20)                 NOT NULL,
    created_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_organization_members PRIMARY KEY (id),
    CONSTRAINT uk_organization_members_organization_user UNIQUE (organization_id, user_id),
    CONSTRAINT fk_organization_members_organization FOREIGN KEY (organization_id) REFERENCES organization (id),
    CONSTRAINT fk_organization_members_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_organization_members_role CHECK (role IN ('OWNER', 'ADMIN', 'USER'))
);

CREATE INDEX idx_organization_members_user_id ON organization_members (user_id);
