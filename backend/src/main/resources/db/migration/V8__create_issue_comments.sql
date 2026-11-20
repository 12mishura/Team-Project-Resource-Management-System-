CREATE TABLE issue_comments
(
    id         UUID                        NOT NULL,
    issue_id   UUID                        NOT NULL,
    author_id  UUID                        NOT NULL,
    text       VARCHAR(5000)               NOT NULL,
    edited_at  TIMESTAMP(6) WITH TIME ZONE,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_issue_comments PRIMARY KEY (id),
    CONSTRAINT fk_issue_comments_issue FOREIGN KEY (issue_id) REFERENCES issues (id) ON DELETE CASCADE,
    CONSTRAINT fk_issue_comments_author FOREIGN KEY (author_id) REFERENCES users (id)
);

CREATE INDEX idx_issue_comments_issue_created ON issue_comments (issue_id, created_at);
CREATE INDEX idx_issue_comments_author_id ON issue_comments (author_id);
