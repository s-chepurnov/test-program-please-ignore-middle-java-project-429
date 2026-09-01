-- Справочник городов. sort_order задаёт порядок выдачи: Москва и Санкт-Петербург идут первыми,
-- потому что фронтенд подставляет в форму поиска первый и второй город из ответа /api/cities.
CREATE TABLE IF NOT EXISTS cities (
    code       text PRIMARY KEY,
    name       text NOT NULL,
    country    text,
    sort_order integer NOT NULL
);

CREATE TABLE IF NOT EXISTS airlines (
    code text PRIMARY KEY,
    name text NOT NULL
);

-- id — текстовый и вычисляется детерминированно из маршрута и времени вылета: заливка идемпотентна
-- (ON CONFLICT DO NOTHING) и повторный запуск не задваивает рейсы.
-- Цена — целое число рублей: numeric драйвер отдаёт как BigDecimal, и Jackson пишет его с масштабом
-- (5400.00), а тип money в PostgreSQL зависит от локали сервера.
CREATE TABLE IF NOT EXISTS flights (
    id               text PRIMARY KEY,
    flight_number    text NOT NULL,
    airline_code     text NOT NULL REFERENCES airlines (code),
    origin_code      text NOT NULL REFERENCES cities (code),
    destination_code text NOT NULL REFERENCES cities (code),
    departure_at     timestamptz NOT NULL,
    arrival_at       timestamptz NOT NULL,
    duration_minutes integer NOT NULL,
    price_amount     integer NOT NULL,
    price_currency   text NOT NULL DEFAULT 'RUB',
    seats_available  integer NOT NULL,
    CONSTRAINT flights_route_check CHECK (origin_code <> destination_code)
);

CREATE INDEX IF NOT EXISTS flights_search_idx
    ON flights (origin_code, destination_code, departure_at);

-- Бронь не удаляется при отмене, у неё меняется status.
-- total_amount хранится в броне: цена рейса со временем меняется, а в броне должна остаться та,
-- по которой купили.
CREATE TABLE IF NOT EXISTS bookings (
    code           text PRIMARY KEY,
    status         text NOT NULL DEFAULT 'confirmed',
    flight_id      text NOT NULL REFERENCES flights (id),
    contact_email  text NOT NULL,
    contact_phone  text NOT NULL,
    total_amount   integer NOT NULL,
    total_currency text NOT NULL DEFAULT 'RUB',
    created_at     timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT bookings_status_check CHECK (status IN ('confirmed', 'cancelled'))
);

CREATE TABLE IF NOT EXISTS booking_passengers (
    id              bigserial PRIMARY KEY,
    booking_code    text NOT NULL REFERENCES bookings (code) ON DELETE CASCADE,
    position        integer NOT NULL,
    first_name      text NOT NULL,
    last_name       text NOT NULL,
    date_of_birth   date NOT NULL,
    document_number text NOT NULL
);

CREATE INDEX IF NOT EXISTS booking_passengers_code_idx
    ON booking_passengers (booking_code, position);

-- Поиск брони идёт по коду и фамилии без учёта регистра — индекс по lower(last_name).
CREATE INDEX IF NOT EXISTS booking_passengers_last_name_idx
    ON booking_passengers (lower(last_name));
