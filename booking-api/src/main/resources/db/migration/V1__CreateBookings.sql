-- Flyway migration V1: создаём таблицу bookings с расширенной схемой
CREATE TABLE IF NOT EXISTS bookings (
    id                 SERIAL             PRIMARY KEY,
    user_id            INT                NOT NULL,
    club_id            INT                NOT NULL,
    table_id           INT                NOT NULL,             -- ID физического стола в клубе
    booking_time       TIMESTAMPTZ        NOT NULL,             -- время прихода
    party_size         INT                NOT NULL,             -- количество гостей
    expected_duration  INT,                                     -- ориентировочное время в минутах
    guest_name         VARCHAR(100),                             -- имя гостя
    telegram_id        BIGINT,                                  -- Telegram ID для связи
    phone              VARCHAR(20),                              -- телефон для связи
    status             VARCHAR(20)         NOT NULL DEFAULT 'PENDING',
    created_at         TIMESTAMPTZ         NOT NULL DEFAULT now()
);

-- Индексы для ускорения выборок
CREATE INDEX IF NOT EXISTS idx_bookings_user_id    ON bookings(user_id);
CREATE INDEX IF NOT EXISTS idx_bookings_club_id    ON bookings(club_id);
CREATE INDEX IF NOT EXISTS idx_bookings_table_id   ON bookings(table_id);
CREATE INDEX IF NOT EXISTS idx_bookings_status     ON bookings(status);