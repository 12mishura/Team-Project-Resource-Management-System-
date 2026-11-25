CREATE TABLE projects
(
    id              UUID                        NOT NULL,
    organization_id UUID                        NOT NULL,
    title           VARCHAR(50)                 NOT NULL,
    description     VARCHAR(400),
    status          VARCHAR(20)                 NOT NULL,
    created_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_projects PRIMARY KEY (id),
    CONSTRAINT fk_projects_organization FOREIGN KEY (organization_id) REFERENCES organization (id),
    CONSTRAINT ck_projects_status CHECK (status IN ('ACTIVE', 'FINISHED', 'ARCHIVED'))
);

CREATE INDEX idx_projects_organization_id ON projects (organization_id);
