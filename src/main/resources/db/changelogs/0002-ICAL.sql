--liquibase formatted sql

--changeset quicklybly:BB-14-create-notification-outbox-status-enum
CREATE TYPE notification_outbox_status AS ENUM ('PENDING', 'IN_PROGRESS', 'DONE', 'FAILED');
--rollback DROP TYPE notification_outbox_status;

--changeset quicklybly:BB-14-create-notification-outbox-type-enum
CREATE TYPE notification_outbox_type AS ENUM ('EMAIL');
--rollback DROP TYPE notification_outbox_type;

--changeset quicklybly:BB-14-create-notification-outbox-table
CREATE TABLE notification_outbox (
    id bigserial NOT NULL,
    status notification_outbox_status NOT NULL DEFAULT 'PENDING',
    type notification_outbox_type NOT NULL,
    payload JSONB NOT NULL,
    attempts_count int DEFAULT 0,
    next_retry_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT notification_outbox_pkey PRIMARY KEY (id)
);

--changeset quicklybly:BB-14-create-notification-outbox-status-idx
CREATE INDEX notification_outbox_polling_idx
ON notification_outbox (next_retry_at ASC, id ASC)
WHERE status = 'PENDING';
--rollback DROP INDEX IF EXISTS notification_outbox_status_idx;

--changeset quicklybly:BB-14-create-notification-outbox-stuck-idx
CREATE INDEX notification_outbox_stuck_idx
ON notification_outbox (updated_at)
WHERE status = 'IN_PROGRESS';
--rollback DROP INDEX IF EXISTS notification_outbox_stuck_idx;

--changeset quicklybly:BB-14-create-notification-outbox-failed-idx
CREATE INDEX notification_outbox_failed_idx
ON notification_outbox (next_retry_at ASC)
WHERE status = 'FAILED';
--rollback DROP INDEX IF EXISTS notification_outbox_failed_idx;
