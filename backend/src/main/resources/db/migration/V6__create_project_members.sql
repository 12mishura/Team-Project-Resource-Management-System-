CREATE TABLE project_members
(
    id                     UUID                        NOT NULL,
    project_id             UUID                        NOT NULL,
    organization_member_id UUID                        NOT NULL,
    role                   VARCHAR(20)                 NOT NULL,
    created_at             TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at             TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_project_members PRIMARY KEY (id),
    CONSTRAINT uk_project_members_project_member UNIQUE (project_id, organization_member_id),
    CONSTRAINT fk_project_members_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_project_members_organization_member FOREIGN KEY (organization_member_id) REFERENCES organization_members (id) ON DELETE CASCADE,
    CONSTRAINT ck_project_members_role CHECK (role IN ('ADMIN', 'EXTENDED', 'USER'))
);

CREATE INDEX idx_project_members_organization_member_id ON project_members (organization_member_id);
