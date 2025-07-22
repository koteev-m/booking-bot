-- Создаем таблицу пользователей
CREATE TABLE IF NOT EXISTS users (
    telegram_id    BIGINT         PRIMARY KEY,
    username       VARCHAR(100),
    role           VARCHAR(20)    NOT NULL DEFAULT 'GUEST',
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now()
);

-- Создаем таблицу клубов
CREATE TABLE IF NOT EXISTS clubs (
    id               SERIAL         PRIMARY KEY,
    name             VARCHAR(100)   NOT NULL,
    description      TEXT,
    admin_channel_id BIGINT
);
