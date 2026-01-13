--liquibase formatted sql

--changeset quicklybly:BB-45-create-meeting-rooms-table
create table if not exists meeting_rooms (
  id uuid primary key,
  name text not null,
  capacity int not null,
  status text not null,

  address text,
  building text,
  floor int,
  wing text,
  room_number text,
  city text,
  time_zone text,

  features_json jsonb not null default '{}'::jsonb
);

create index if not exists idx_meeting_rooms_capacity on meeting_rooms (capacity);
create index if not exists idx_meeting_rooms_status on meeting_rooms (status);
create index if not exists idx_meeting_rooms_floor on meeting_rooms (floor);
create index if not exists idx_meeting_rooms_building on meeting_rooms (building);

--changeset quicklybly:BB-45-create-meeting-room-bookings-table
create table if not exists meeting_room_bookings (
  id uuid primary key,
  room_id uuid not null references meeting_rooms(id),
  organizer_id uuid not null,

  purpose text,
  status text not null,

  start_time timestamptz not null,
  end_time timestamptz not null
);

create index if not exists idx_bookings_room_time
  on meeting_room_bookings (room_id, start_time, end_time);

--changeset quicklybly:BB-45-prevent-overlaps-in-meeting-room-bookings
create extension if not exists btree_gist;

alter table meeting_room_bookings
  add constraint ex_bookings_no_overlap
  exclude using gist (
    room_id with =,
    tstzrange(start_time, end_time, '[)') with &&
  )
  where (status = 'CONFIRMED');
