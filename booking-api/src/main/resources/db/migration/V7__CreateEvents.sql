-- Создаем таблицу для хранения афиш и событий
CREATE TABLE IF NOT EXISTS events (
    id            SERIAL         PRIMARY KEY,
    club_id       INT            NOT NULL REFERENCES clubs(id),
    title         VARCHAR(255)   NOT NULL,
    description   TEXT,
    event_date    TIMESTAMPTZ    NOT NULL,
    image_url     VARCHAR(512), -- Ссылка на изображение афиши
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now()
);

-- Индекс для быстрого поиска событий по клубу и дате
CREATE INDEX IF NOT EXISTS idx_events_club_id_date ON events(club_id, event_date);