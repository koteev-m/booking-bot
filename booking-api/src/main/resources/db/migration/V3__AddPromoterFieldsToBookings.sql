-- Добавляем колонки для поддержки бронирований от промоутеров
ALTER TABLE bookings
ADD COLUMN promoter_id BIGINT,
ADD CONSTRAINT fk_promoter_id FOREIGN KEY (promoter_id) REFERENCES users(telegram_id);

-- Переименовываем guest_name, чтобы было понятнее, что это имя гостя, а не пользователя
ALTER TABLE bookings
RENAME COLUMN guest_name TO booking_guest_name;