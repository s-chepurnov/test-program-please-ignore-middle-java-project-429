package io.hexlet.flightbooking.seed;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class Seeder {

    public static final int HORIZON_DAYS = 30;
    private static final int BATCH_SIZE = 250;

    private static final List<String[]> CITIES = List.of(
            new String[] {"MOW", "Москва", "Россия"},
            new String[] {"LED", "Санкт-Петербург", "Россия"},
            new String[] {"AER", "Сочи", "Россия"},
            new String[] {"KZN", "Казань", "Россия"},
            new String[] {"SVX", "Екатеринбург", "Россия"},
            new String[] {"OVB", "Новосибирск", "Россия"},
            new String[] {"KGD", "Калининград", "Россия"});

    private static final List<String[]> AIRLINES = List.of(
            new String[] {"SU", "Аэрофлот"},
            new String[] {"DP", "Победа"},
            new String[] {"S7", "S7 Airlines"},
            new String[] {"U6", "Уральские авиалинии"});

    private final JdbcTemplate jdbc;

    public Seeder(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int seed() {
        return seed(LocalDate.now(ZoneOffset.UTC));
    }

    public int seed(LocalDate today) {
        seedCities();
        seedAirlines();
        return insertFlights(buildFlights(today));
    }

    private void seedCities() {
        var sql = """
            INSERT INTO cities (code, name, country, sort_order) VALUES (?, ?, ?, ?)
            ON CONFLICT (code) DO UPDATE
                SET name = EXCLUDED.name,
                    country = EXCLUDED.country,
                    sort_order = EXCLUDED.sort_order
            """;
        jdbc.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
                var city = CITIES.get(i);
                ps.setString(1, city[0]);
                ps.setString(2, city[1]);
                ps.setString(3, city[2]);
                ps.setInt(4, i);
            }
            @Override
            public int getBatchSize() { return CITIES.size(); }
        });
    }

    private void seedAirlines() {
        var sql = """
            INSERT INTO airlines (code, name) VALUES (?, ?)
            ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name
            """;
        jdbc.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
                var airline = AIRLINES.get(i);
                ps.setString(1, airline[0]);
                ps.setString(2, airline[1]);
            }
            @Override
            public int getBatchSize() { return AIRLINES.size(); }
        });
    }

    private record Flight(
            String id, String flightNumber, String airlineCode,
            String origin, String destination,
            LocalDateTime departure, LocalDateTime arrival,
            int durationMinutes, int priceAmount, int seatsAvailable) {}

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

    private int insertFlights(List<Flight> flights) {
        var sql = """
            INSERT INTO flights
                (id, flight_number, airline_code, origin_code, destination_code,
                 departure_at, arrival_at, duration_minutes, price_amount, seats_available)
            VALUES (?, ?, ?, ?, ?, ?::timestamptz, ?::timestamptz, ?, ?, ?)
            ON CONFLICT (id) DO NOTHING
            """;
        var inserted = 0;
        for (var start = 0; start < flights.size(); start += BATCH_SIZE) {
            var end = Math.min(start + BATCH_SIZE, flights.size());
            var chunk = flights.subList(start, end);
            var results = jdbc.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
                    var f = chunk.get(i);
                    ps.setString(1, f.id());
                    ps.setString(2, f.flightNumber());
                    ps.setString(3, f.airlineCode());
                    ps.setString(4, f.origin());
                    ps.setString(5, f.destination());
                    ps.setString(6, f.departure().atOffset(ZoneOffset.UTC).toString());
                    ps.setString(7, f.arrival().atOffset(ZoneOffset.UTC).toString());
                    ps.setInt(8, f.durationMinutes());
                    ps.setInt(9, f.priceAmount());
                    ps.setInt(10, f.seatsAvailable());
                }
                @Override
                public int getBatchSize() { return chunk.size(); }
            });
            for (var r : results) if (r > 0) inserted += r;
        }
        return inserted;
    }

    static String flightId(String origin, String destination, LocalDateTime departure) {
        var key = origin + destination + departure.atOffset(ZoneOffset.UTC);
        try {
            var digest = MessageDigest.getInstance("SHA-1")
                    .digest(key.getBytes(StandardCharsets.UTF_8));
            return "fl_" + HexFormat.of().formatHex(digest).substring(0, 12);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
