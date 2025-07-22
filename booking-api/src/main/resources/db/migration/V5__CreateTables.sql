-- Создаем таблицу для столов
CREATE TABLE IF NOT EXISTS tables (
    id            SERIAL         PRIMARY KEY,
    club_id       INT            NOT NULL REFERENCES clubs(id),
    table_number  INT            NOT NULL,
    capacity      INT            NOT NULL,
    min_deposit   DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    UNIQUE(club_id, table_number)
);

-- Добавляем несколько столов для примера
INSERT INTO tables (club_id, table_number, capacity, min_deposit) VALUES
(1, 1, 4, 5000.00),
(1, 2, 4, 5000.00),
(1, 3, 6, 8000.00),
(2, 1, 2, 3000.00),
(2, 2, 8, 10000.00);