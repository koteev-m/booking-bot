-- Добавляем колонку для источника бронирования
ALTER TABLE bookings
ADD COLUMN source VARCHAR(100) NOT NULL DEFAULT 'Бот';