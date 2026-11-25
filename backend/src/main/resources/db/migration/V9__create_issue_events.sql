CREATE TABLE issue_events
(
    id         UUID                        NOT NULL,
    issue_id   UUID                        NOT NULL,
    actor_id   UUID                        NOT NULL,
    type       VARCHAR(30)                 NOT NULL,
    field      VARCHAR(50),
    old_value  VARCHAR(5000),
    new_value  VARCHAR(5000),
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_issue_events PRIMARY KEY (id),
    CONSTRAINT fk_issue_events_issue FOREIGN KEY (issue_id) REFERENCES issues (id) ON DELETE CASCADE,
    CONSTRAINT fk_issue_events_actor FOREIGN KEY (actor_id) REFERENCES users (id),
    CONSTRAINT ck_issue_events_type CHECK (type IN ('CREATED', 'FIELD_CHANGED', 'STATUS_CHANGED', 'ASSIGNEE_CHANGED', 'PARENT_CHANGED'))
);

CREATE INDEX idx_issue_events_issue_created ON issue_events (issue_id, created_at);
CREATE INDEX idx_issue_events_actor_id ON issue_events (actor_id);
