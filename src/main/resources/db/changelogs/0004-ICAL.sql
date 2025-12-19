--liquibase formatted sql

--changeset quicklybly:BB-24-create-user-oauth-links-table
CREATE TABLE user_oauth_links (
    id bigserial PRIMARY KEY,
    user_login VARCHAR(100) NOT NULL,
    client_registration_id VARCHAR(100) NOT NULL,
    external_principal_name VARCHAR(200) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

    CONSTRAINT uq_user_provider UNIQUE (user_login, client_registration_id),

    CONSTRAINT fk_user_oauth_links_users FOREIGN KEY (user_login) REFERENCES users(login) ON DELETE CASCADE,
    CONSTRAINT fk_user_oauth_links_tokens
        FOREIGN KEY (client_registration_id, external_principal_name)
        REFERENCES oauth2_authorized_client (client_registration_id, principal_name)
        ON DELETE CASCADE
);
