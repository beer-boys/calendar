--liquibase formatted sql

--changeset a.k.lysenko:1 logicalFilePath:db/changelogs/0001-ICAL.sql

CREATE TABLE users
(
    id         UUID PRIMARY KEY,
    login      text NOT NULL CHECK ( length(login) > 0 AND length(login) < 100 ),
    password   text NOT NULL CHECK ( length(password) > 0),
    first_name  text NOT NULL CHECK ( length(first_name) > 0),
    last_name   text NOT NULL CHECK ( length(last_name) > 0),
    middle_name text NOT NULL CHECK ( length(middle_name) > 0)
);

CREATE UNIQUE INDEX users_login_idx ON users (login);

CREATE TABLE roles
(
    id        SERIAL PRIMARY KEY,
    role_name text NOT NULL CHECK ( length(role_name) > 0 )
);

CREATE TABLE users_roles
(
    user_id UUID REFERENCES users (id),
    role_id INT REFERENCES roles (id),
    PRIMARY KEY (user_id, role_id)
);

INSERT INTO roles
VALUES (1, 'USER'), (2, 'ADMIN');