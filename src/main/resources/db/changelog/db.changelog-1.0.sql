--liquibase formatted sql

--changeset dab1:1
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(125) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
)