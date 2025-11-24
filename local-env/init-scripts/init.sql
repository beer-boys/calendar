CREATE USER calendar WITH
    LOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    INHERIT
    NOREPLICATION
    PASSWORD 'calendar';

GRANT CONNECT ON DATABASE calendar TO calendar;

CREATE SCHEMA calendar
    AUTHORIZATION calendar;

GRANT USAGE ON SCHEMA calendar TO calendar;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA calendar TO calendar;


-- liquibase user
CREATE USER liquibase WITH
    LOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    INHERIT
    NOREPLICATION
    PASSWORD 'liquibase';

GRANT CONNECT ON DATABASE calendar TO liquibase;

COMMENT ON ROLE liquibase
    IS 'Пользователь для обновления DML';

GRANT USAGE, CREATE ON SCHEMA calendar TO liquibase;

ALTER DEFAULT PRIVILEGES
    FOR USER liquibase
    IN SCHEMA calendar
    GRANT ALL PRIVILEGES ON TABLES TO calendar;
