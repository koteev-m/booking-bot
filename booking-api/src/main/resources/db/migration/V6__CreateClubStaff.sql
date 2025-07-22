-- Создаем таблицу для связи пользователей (персонала) с клубами
CREATE TABLE IF NOT EXISTS club_staff (
    user_id    BIGINT NOT NULL REFERENCES users(telegram_id),
    club_id    INT    NOT NULL REFERENCES clubs(id),
    -- Роль в рамках клуба может отличаться от общей роли, но пока не усложняем
    PRIMARY KEY (user_id, club_id)
);

-- Индекс для быстрого поиска персонала по клубу
CREATE INDEX IF NOT EXISTS idx_club_staff_club_id ON club_staff(club_id);
