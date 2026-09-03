package io.hexlet.flightbooking;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;

public final class Repository {

    private static final String CODE_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;
    private static final int CODE_ATTEMPTS = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String FLIGHT_SELECT =
            """
            SELECT f.id,
                   f.flight_number,
                   f.duration_minutes,
                   f.price_amount,
                   f.price_currency,
                   f.seats_available,
                   to_char(f.departure_at AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS departure_at,
                   to_char(f.arrival_at   AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS arrival_at,
                   a.code AS airline_code,
                   a.name AS airline_name,
                   o.code AS origin_code,
                   o.name AS origin_name,
                   o.country AS origin_country,
                   d.code AS destination_code,
                   d.name AS destination_name,
                   d.country AS destination_country
              FROM flights f
              JOIN airlines a ON a.code = f.airline_code
              JOIN cities   o ON o.code = f.origin_code
              JOIN cities   d ON d.code = f.destination_code
            """;

    private static final String BOOKING_SELECT =
            """
            SELECT b.code,
                   b.status,
                   b.flight_id,
                   b.contact_email,
                   b.contact_phone,
                   b.total_amount,
                   b.total_currency,
                   to_char(b.created_at AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS"Z"') AS created_at
              FROM bookings b
             WHERE b.code = ?
               AND EXISTS (
                     SELECT 1 FROM booking_passengers p
                      WHERE p.booking_code = b.code
                        AND lower(p.last_name) = lower(btrim(?))
                   )
            """;

    private final DataSource dataSource;

    public Repository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static String generateCode() {
        var code = new StringBuilder(CODE_LENGTH);
        for (var i = 0; i < CODE_LENGTH; i++) {
            code.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }

        return code.toString();
    }

    public List<Map<String, Object>> cities() throws SQLException {
        var cities = new ArrayList<Map<String, Object>>();

        try (var connection = dataSource.getConnection();
                var statement =
                        connection.prepareStatement(
                                "SELECT code, name, country FROM cities ORDER BY sort_order");
                var rows = statement.executeQuery()) {
            while (rows.next()) {
                cities.add(Presenter.city(rows));
            }
        }

        return cities;
    }

    public List<Map<String, Object>> searchFlights(
            String origin, String destination, String date, int passengers) throws SQLException {
        var sql =
                FLIGHT_SELECT
                        + """
                         WHERE f.origin_code = ?
                           AND f.destination_code = ?
                           AND (f.departure_at AT TIME ZONE 'UTC')::date = ?::date
                           AND f.seats_available >= ?
                         ORDER BY f.departure_at
                        """;
        var flights = new ArrayList<Map<String, Object>>();

        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement(sql)) {
            statement.setString(1, origin);
            statement.setString(2, destination);
            statement.setString(3, date);
            statement.setInt(4, passengers);

            try (var rows = statement.executeQuery()) {
                while (rows.next()) {
                    flights.add(Presenter.flight(rows));
                }
            }
        }

        return flights;
    }

    public Optional<Map<String, Object>> findFlight(String id) throws SQLException {
        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement(FLIGHT_SELECT + " WHERE f.id = ?")) {
            statement.setString(1, id);

            try (var rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(Presenter.flight(rows)) : Optional.empty();
            }
        }
    }

    public boolean flightExists(String id) throws SQLException {
        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement("SELECT 1 FROM flights WHERE id = ?")) {
            statement.setString(1, id);

            try (var rows = statement.executeQuery()) {
                return rows.next();
            }
        }
    }

    public String createBooking(
            String flightId, Validator.Contact contact, List<Validator.Passenger> passengers)
            throws SQLException {
        try (var connection = dataSource.getConnection()) {
            int priceAmount;
            String currency;

            try (var statement =
                            connection.prepareStatement(
                                    "SELECT price_amount, price_currency FROM flights WHERE id = ?");
                    var rows = execute(statement, flightId)) {
                if (!rows.next()) {
                    throw new SQLException("Рейс " + flightId + " не найден");
                }
                priceAmount = rows.getInt("price_amount");
                currency = rows.getString("price_currency");
            }

            var total = priceAmount * passengers.size();

            connection.setAutoCommit(false);
            try {
                var code = insertBooking(connection, flightId, contact, total, currency);
                insertPassengers(connection, code, passengers);
                connection.commit();

                return code;
            } catch (SQLException error) {
                connection.rollback();

                throw error;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public Optional<Map<String, Object>> findBooking(String code, String lastName)
            throws SQLException {
        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement(BOOKING_SELECT)) {
            statement.setString(1, code);
            statement.setString(2, lastName);

            try (var rows = statement.executeQuery()) {
                if (!rows.next()) {
                    return Optional.empty();
                }

                var flightId = rows.getString("flight_id");
                var flight = findFlight(flightId);
                if (flight.isEmpty()) {
                    return Optional.empty();
                }

                return Optional.of(
                        Presenter.booking(
                                rows,
                                flight.get(),
                                passengersOf(connection, rows.getString("code"))));
            }
        }
    }

    public void cancelBooking(String code) throws SQLException {
        try (var connection = dataSource.getConnection();
                var statement =
                        connection.prepareStatement(
                                "UPDATE bookings SET status = 'cancelled' WHERE code = ?")) {
            statement.setString(1, code);
            statement.executeUpdate();
        }
    }

    private List<Map<String, Object>> passengersOf(Connection connection, String code)
            throws SQLException {
        var passengers = new ArrayList<Map<String, Object>>();

        try (var statement =
                        connection.prepareStatement(
                                """
                                SELECT first_name,
                                       last_name,
                                       to_char(date_of_birth, 'YYYY-MM-DD') AS date_of_birth,
                                       document_number
                                  FROM booking_passengers
                                 WHERE booking_code = ?
                                 ORDER BY position
                                """);
                var rows = execute(statement, code)) {
            while (rows.next()) {
                passengers.add(Presenter.passenger(rows));
            }
        }

        return passengers;
    }

    private String insertBooking(
            Connection connection,
            String flightId,
            Validator.Contact contact,
            int total,
            String currency)
            throws SQLException {
        var sql =
                """
                INSERT INTO bookings
                    (code, status, flight_id, contact_email, contact_phone, total_amount, total_currency)
                VALUES (?, 'confirmed', ?, ?, ?, ?, ?)
                ON CONFLICT (code) DO NOTHING
                """;

        for (var attempt = 0; attempt < CODE_ATTEMPTS; attempt++) {
            var candidate = generateCode();

            try (var statement = connection.prepareStatement(sql)) {
                statement.setString(1, candidate);
                statement.setString(2, flightId);
                statement.setString(3, contact.email());
                statement.setString(4, contact.phone());
                statement.setInt(5, total);
                statement.setString(6, currency);

                if (statement.executeUpdate() == 1) {
                    return candidate;
                }
            }
        }

        throw new SQLException("Не удалось выделить уникальный код брони");
    }

    private void insertPassengers(
            Connection connection, String code, List<Validator.Passenger> passengers)
            throws SQLException {
        var sql =
                """
                INSERT INTO booking_passengers
                    (booking_code, position, first_name, last_name, date_of_birth, document_number)
                VALUES (?, ?, ?, ?, ?::date, ?)
                """;

        try (var statement = connection.prepareStatement(sql)) {
            for (var position = 0; position < passengers.size(); position++) {
                var passenger = passengers.get(position);
                statement.setString(1, code);
                statement.setInt(2, position);
                statement.setString(3, passenger.firstName());
                statement.setString(4, passenger.lastName());
                statement.setString(5, passenger.dateOfBirth());
                statement.setString(6, passenger.documentNumber());
                statement.addBatch();
            }

            statement.executeBatch();
        }
    }

    private java.sql.ResultSet execute(PreparedStatement statement, String parameter)
            throws SQLException {
        statement.setString(1, parameter);

        return statement.executeQuery();
    }
}
