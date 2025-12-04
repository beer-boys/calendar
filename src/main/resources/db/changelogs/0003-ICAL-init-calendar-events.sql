--liquibase formatted sql

--changeset nex0rus:BB-32-create-entity-type-enum
CREATE TYPE entity_type AS ENUM ('HABIT', 'MEETING', 'FOCUS_TIME');
--rollback DROP TYPE entity_type;

--changeset nex0rus:BB-32-create-calendar-events-table
CREATE TABLE calendar_events (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    entity_type entity_type NOT NULL,
    external_event_id TEXT,
    title TEXT NOT NULL CHECK (length(title) > 0),
    description TEXT,
    priority INT NOT NULL DEFAULT 5 CHECK (priority >= 0 AND priority <= 10),
    metadata JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
--rollback DROP TABLE calendar_events;

--changeset nex0rus:BB-32-create-calendar-events-user-idx
CREATE INDEX calendar_events_user_id_idx ON calendar_events(user_id);
--rollback DROP INDEX IF EXISTS calendar_events_user_id_idx;

--changeset nex0rus:BB-32-create-calendar-events-entity-type-idx
CREATE INDEX calendar_events_entity_type_idx ON calendar_events(entity_type);
--rollback DROP INDEX IF EXISTS calendar_events_entity_type_idx;

--changeset nex0rus:BB-32-create-calendar-events-external-event-idx
CREATE INDEX calendar_events_external_event_id_idx ON calendar_events(external_event_id)
WHERE external_event_id IS NOT NULL;
--rollback DROP INDEX IF EXISTS calendar_events_external_event_id_idx;

--changeset nex0rus:BB-32-create-calendar-events-metadata-gin-idx
CREATE INDEX calendar_events_metadata_gin_idx ON calendar_events USING GIN (metadata);
--rollback DROP INDEX IF EXISTS calendar_events_metadata_gin_idx;

