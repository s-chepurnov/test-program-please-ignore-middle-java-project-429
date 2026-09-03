package io.hexlet.flightbooking;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import javax.sql.DataSource;

public final class Seeder {

    public static final int HORIZON_DAYS = 30;
    private static final int BATCH_SIZE = 250;

    private static final List<String[]> CITIES =
            List.of(
                    new String[] {"MOW", "Москва", "Россия"},
                    new String[] {"LED", "Санкт-Петербург", "Россия"},
                    new String[] {"AER", "Сочи", "Россия"},
                    new String[] {"KZN", "Казань", "Россия"},
                    new String[] {"SVX", "Екатеринбург", "Россия"},
                    new String[] {"OVB", "Новосибирск", "Россия"},
                    new String[] {"KGD", "Калининград", "Россия"});

    private static final List<String[]> AIRLINES =
            List.of(
                    new String[] {"SU", "Аэрофлот"},
                    new String[] {"DP", "Победа"},
                    new String[] {"S7", "S7 Airlines"},
                    new String[] {"U6", "Уральские авиалинии"});

    private final DataSource dataSource;

    public Seeder(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private record Flight(
            String id,
            String flightNumber,
            String airlineCode,
            String origin,
            String destination,
            LocalDateTime departure,
            LocalDateTime arrival,
            int durationMinutes,
            int priceAmount,
            int seatsAvailable) {}

    public int seed() throws SQLException {
        return seed(LocalDate.now(ZoneOffset.UTC));
    }

    public int seed(LocalDate today) throws SQLException {
        try (var connection = Database.connectWithRetries(dataSource)) {
            seedCities(connection);
            seedAirlines(connection);

            return insertFlights(connection, buildFlights(today));
        }
    }

    static String flightId(String origin, String destination, LocalDateTime departure) {
        var key = origin + destination + departure.atOffset(ZoneOffset.UTC);

        try {
            var digest =
                    MessageDigest.getInstance("SHA-1").digest(key.getBytes(StandardCharsets.UTF_8));

            return "fl_" + HexFormat.of().formatHex(digest).substring(0, 12);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }

    private void seedCities(Connection connection) throws SQLException {
        var sql =
                """
                INSERT INTO cities (code, name, country, sort_order) VALUES (?, ?, ?, ?)
                ON CONFLICT (code) DO UPDATE
                   SET name = EXCLUDED.name,
                       country = EXCLUDED.country,
                       sort_order = EXCLUDED.sort_order
                """;

        try (var statement = connection.prepareStatement(sql)) {
            for (var order = 0; order < CITIES.size(); order++) {
                var city = CITIES.get(order);
                statement.setString(1, city[0]);
                statement.setString(2, city[1]);
                statement.setString(3, city[2]);
                statement.setInt(4, order);
                statement.addBatch();
            }

            statement.executeBatch();
        }
    }

    private void seedAirlines(Connection connection) throws SQLException {
        var sql =
                """
                INSERT INTO airlines (code, name) VALUES (?, ?)
                ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name
                """;

        try (var statement = connection.prepareStatement(sql)) {
            for (var airline : AIRLINES) {
                statement.setString(1, airline[0]);
                statement.setString(2, airline[1]);
                statement.addBatch();
            }

            statement.executeBatch();
        }
    }

    private List<Flight> buildFlights(LocalDate today) {
        var flights = new ArrayList<Flight>();
        var counter = 0;

        for (var from : CITIES) {
            for (var to : CITIES) {
                var origin = from[0];
                var destination = to[0];
                if (origin.equals(destination)) {
                    continue;
                }

                // Сид маршрута — из его кодов: набор рейсов на маршруте стабилен между запусками.
                var seed = 0;
                for (var character : (origin + destination).toCharArray()) {
                    seed += character;
                }
                var rng = new Rng(seed);

                for (var day = 0; day < HORIZON_DAYS; day++) {
                    var perDay = 2 + (int) (rng.next() * 2); // 2–3 рейса в день

                    for (var index = 0; index < perDay; index++) {
                        counter++;
                        var minutes = (int) (rng.next() * 4) * 15;
                        var departure = today.plusDays(day).atTime(6 + index * 5, minutes);
                        var duration = 80 + (int) (rng.next() * 200);
                        var airlineCode = AIRLINES.get(counter % AIRLINES.size())[0];

                        flights.add(
                                new Flight(
                                        flightId(origin, destination, departure),
                                        airlineCode + (1000 + counter % 9000),
                                        airlineCode,
                                        origin,
                                        destination,
                                        departure,
                                        departure.plusMinutes(duration),
                                        duration,
                                        3000 + (int) (rng.next() * 12) * 500,
                                        10 + (int) (rng.next() * 80)));
                    }
                }
            }
        }

        return flights;
    }

    private int insertFlights(Connection connection, List<Flight> flights) throws SQLException {
        var sql =
                """
                INSERT INTO flights
                    (id, flight_number, airline_code, origin_code, destination_code,
                     departure_at, arrival_at, duration_minutes, price_amount, seats_available)
                VALUES (?, ?, ?, ?, ?, ?::timestamptz, ?::timestamptz, ?, ?, ?)
                ON CONFLICT (id) DO NOTHING
                """;
        var inserted = 0;

        try (var statement = connection.prepareStatement(sql)) {
            for (var index = 0; index < flights.size(); index++) {
                var flight = flights.get(index);
                statement.setString(1, flight.id());
                statement.setString(2, flight.flightNumber());
                statement.setString(3, flight.airlineCode());
                statement.setString(4, flight.origin());
                statement.setString(5, flight.destination());
                statement.setString(6, flight.departure().atOffset(ZoneOffset.UTC).toString());
                statement.setString(7, flight.arrival().atOffset(ZoneOffset.UTC).toString());
                statement.setInt(8, flight.durationMinutes());
                statement.setInt(9, flight.priceAmount());
                statement.setInt(10, flight.seatsAvailable());
                statement.addBatch();

                // Пачками, а не одним батчем на 3000+ строк: драйвер держит их в памяти целиком.
                if ((index + 1) % BATCH_SIZE == 0) {
                    inserted += count(statement.executeBatch());
                }
            }

            inserted += count(statement.executeBatch());
        }

        return inserted;
    }

    private int count(int[] results) {
        var inserted = 0;
        for (var result : results) {
            if (result > 0) {
                inserted += result;
            }
        }

        return inserted;
    }
}
