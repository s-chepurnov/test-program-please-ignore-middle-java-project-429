-- Очищает всё, что создаётся тестами.
-- Seeder-данные (cities, airlines, flights) не трогаем — они нужны всем тестам.
TRUNCATE TABLE booking_passengers, bookings RESTART IDENTITY CASCADE;
