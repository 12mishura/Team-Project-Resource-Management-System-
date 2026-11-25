ALTER TABLE issues ADD COLUMN estimate_minutes INTEGER;
ALTER TABLE issues ADD CONSTRAINT ck_issues_estimate
    CHECK (estimate_minutes IS NULL OR (type <> 'EPIC' AND estimate_minutes BETWEEN 1 AND 60000));

ALTER TABLE organization_members ADD COLUMN weekly_capacity_minutes INTEGER NOT NULL DEFAULT 2400;
ALTER TABLE organization_members ADD CONSTRAINT ck_organization_members_capacity
    CHECK (weekly_capacity_minutes BETWEEN 0 AND 10080);

CREATE TABLE work_logs
(
    id         UUID                        NOT NULL,
    issue_id   UUID                        NOT NULL,
    author_id  UUID                        NOT NULL,
    work_date  DATE                        NOT NULL,
    minutes    INTEGER                     NOT NULL,
    comment    VARCHAR(500),
    version    BIGINT                      NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_work_logs PRIMARY KEY (id),
    CONSTRAINT fk_work_logs_issue FOREIGN KEY (issue_id) REFERENCES issues (id) ON DELETE CASCADE,
    CONSTRAINT fk_work_logs_author FOREIGN KEY (author_id) REFERENCES users (id),
    CONSTRAINT ck_work_logs_minutes CHECK (minutes BETWEEN 1 AND 1440)
);

CREATE INDEX idx_work_logs_issue_id ON work_logs (issue_id);
CREATE INDEX idx_work_logs_author_date ON work_logs (author_id, work_date);
CREATE INDEX idx_work_logs_date ON work_logs (work_date);

ALTER TABLE issue_events DROP CONSTRAINT ck_issue_events_type;
ALTER TABLE issue_events ADD CONSTRAINT ck_issue_events_type
    CHECK (type IN ('CREATED', 'FIELD_CHANGED', 'STATUS_CHANGED', 'ASSIGNEE_CHANGED', 'PARENT_CHANGED', 'WORK_LOGGED'));
