--liquibase formatted sql

--changeset nex0rus:BB-33-create-occurrence-status-enum
CREATE TYPE occurrence_status AS ENUM ('SCHEDULED', 'UNSCHEDULED', 'CANCELLED', 'SYNCED');
--rollback DROP TYPE occurrence_status;

--changeset nex0rus:BB-33-create-habit-occurrences-table
CREATE TABLE habit_occurrences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    habit_id UUID NOT NULL REFERENCES calendar_events(id) ON DELETE CASCADE,
    occurrence_date DATE NOT NULL,
    status occurrence_status NOT NULL DEFAULT 'SCHEDULED',
    slot_start TIMESTAMP WITH TIME ZONE,
    slot_end TIMESTAMP WITH TIME ZONE,
    external_event_id TEXT,
    reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT habit_occurrences_unique_date UNIQUE (habit_id, occurrence_date)
);
--rollback DROP TABLE habit_occurrences;

--changeset nex0rus:BB-33-create-habit-occurrences-habit-idx
CREATE INDEX habit_occurrences_habit_id_idx ON habit_occurrences(habit_id);
--rollback DROP INDEX IF EXISTS habit_occurrences_habit_id_idx;

--changeset nex0rus:BB-33-create-habit-occurrences-date-idx
CREATE INDEX habit_occurrences_date_idx ON habit_occurrences(occurrence_date);
--rollback DROP INDEX IF EXISTS habit_occurrences_date_idx;

--changeset nex0rus:BB-33-create-habit-occurrences-external-event-idx
CREATE INDEX habit_occurrences_external_event_id_idx ON habit_occurrences(external_event_id)
WHERE external_event_id IS NOT NULL;
--rollback DROP INDEX IF EXISTS habit_occurrences_external_event_id_idx;

--changeset nex0rus:BB-33-create-habit-occurrences-status-idx
CREATE INDEX habit_occurrences_status_idx ON habit_occurrences(status);
--rollback DROP INDEX IF EXISTS habit_occurrences_status_idx;

