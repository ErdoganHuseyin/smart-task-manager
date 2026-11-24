CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL
);

-- Tasks created before login existed have no owner. They are local test data,
-- so we remove them instead of inventing a fake owner.
DELETE FROM tasks;

-- Every task now belongs to exactly one user. Deleting a user deletes their tasks.
ALTER TABLE tasks
    ADD COLUMN user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE;

-- Every task query now starts with "WHERE user_id = ?", often followed by "AND status = ?".
-- A composite index on (user_id, status) serves both, so the old status-only index is no longer needed.
DROP INDEX idx_tasks_status;
CREATE INDEX idx_tasks_user_status ON tasks (user_id, status);
