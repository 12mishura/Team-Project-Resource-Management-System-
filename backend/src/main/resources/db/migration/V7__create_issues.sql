CREATE TABLE issues
(
    id                 UUID                        NOT NULL,
    type               VARCHAR(20)                 NOT NULL,
    project_id         UUID                        NOT NULL,
    parent_id          UUID,
    reporter_id        UUID                        NOT NULL,
    assignee_id        UUID,
    title              VARCHAR(200)                NOT NULL,
    description        VARCHAR(5000),
    status             VARCHAR(20)                 NOT NULL,
    priority           VARCHAR(20)                 NOT NULL,
    severity           VARCHAR(20),
    steps_to_reproduce VARCHAR(5000),
    story_points       INTEGER,
    version            BIGINT                      NOT NULL,
    created_at         TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at         TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_issues PRIMARY KEY (id),
    CONSTRAINT fk_issues_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_issues_parent FOREIGN KEY (parent_id) REFERENCES issues (id) ON DELETE SET NULL,
    CONSTRAINT fk_issues_reporter FOREIGN KEY (reporter_id) REFERENCES users (id),
    CONSTRAINT fk_issues_assignee FOREIGN KEY (assignee_id) REFERENCES project_members (id) ON DELETE SET NULL,
    CONSTRAINT ck_issues_type CHECK (type IN ('EPIC', 'STORY', 'TASK', 'BUG')),
    CONSTRAINT ck_issues_status CHECK (status IN ('TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE')),
    CONSTRAINT ck_issues_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_issues_severity CHECK (severity IN ('MINOR', 'MAJOR', 'CRITICAL', 'BLOCKER')),
    CONSTRAINT ck_issues_bug_fields CHECK ((type = 'BUG' AND severity IS NOT NULL)
        OR (type <> 'BUG' AND severity IS NULL AND steps_to_reproduce IS NULL)),
    CONSTRAINT ck_issues_story_points CHECK ((type = 'STORY' AND (story_points IS NULL OR story_points BETWEEN 0 AND 100))
        OR (type <> 'STORY' AND story_points IS NULL)),
    CONSTRAINT ck_issues_epic_without_parent CHECK (type <> 'EPIC' OR parent_id IS NULL)
);

CREATE INDEX idx_issues_project_status ON issues (project_id, status);
CREATE INDEX idx_issues_project_type ON issues (project_id, type);
CREATE INDEX idx_issues_assignee_id ON issues (assignee_id);
CREATE INDEX idx_issues_parent_id ON issues (parent_id);
CREATE INDEX idx_issues_reporter_id ON issues (reporter_id);
