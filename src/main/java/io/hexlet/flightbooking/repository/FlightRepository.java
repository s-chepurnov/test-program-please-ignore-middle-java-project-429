package io.hexlet.flightbooking.repository;

import io.hexlet.flightbooking.model.Flight;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class FlightRepository {

    private static final String TIME_FMT = "YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"";

    private static final String SELECT_FLIGHT = """
        SELECT id,
               flight_number,
               airline_code,
               origin_code,
               destination_code,
               to_char(departure_at AT TIME ZONE 'UTC', '%s') AS departure_at,
               to_char(arrival_at   AT TIME ZONE 'UTC', '%s') AS arrival_at,
               duration_minutes,
               price_amount,
               price_currency,
               seats_available
          FROM flights
        """.formatted(TIME_FMT, TIME_FMT);

    private static final RowMapper<Flight> ROW_MAPPER = (rs, rowNum) -> new Flight(
            rs.getString("id"),
            rs.getString("flight_number"),
            rs.getString("airline_code"),
            rs.getString("origin_code"),
            rs.getString("destination_code"),
            rs.getString("departure_at"),
            rs.getString("arrival_at"),
            rs.getInt("duration_minutes"),
            rs.getInt("price_amount"),
            rs.getString("price_currency"),
            rs.getInt("seats_available")
    );

    private final JdbcTemplate jdbc;

    public FlightRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Flight> search(String origin, String destination,
                               LocalDate date, int passengers) {
        return jdbc.query(SELECT_FLIGHT + """
            WHERE origin_code = ?
              AND destination_code = ?
              AND departure_at::date = ?
              AND seats_available >= ?
            ORDER BY departure_at
            """, ROW_MAPPER, origin, destination, date, passengers);
    }

    public Optional<Flight> findById(String id) {
        return jdbc.query(SELECT_FLIGHT + " WHERE id = ?", ROW_MAPPER, id)
                .stream().findFirst();
    }

    /** Атомарно списывает места. Возвращает 0, если мест не хватило. */
    public int decrementSeats(String flightId, int count) {
        return jdbc.update("""
            UPDATE flights
               SET seats_available = seats_available - ?
             WHERE id = ?
               AND seats_available >= ?
            """, count, flightId, count);
    }

    /** Возвращает места при отмене брони. */
    public int incrementSeats(String flightId, int count) {
        return jdbc.update(
                "UPDATE flights SET seats_available = seats_available + ? WHERE id = ?",
                count, flightId);
    }
}
