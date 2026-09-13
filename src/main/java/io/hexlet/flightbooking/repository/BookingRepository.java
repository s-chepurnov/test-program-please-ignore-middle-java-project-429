package io.hexlet.flightbooking.repository;

import io.hexlet.flightbooking.model.Booking;
import io.hexlet.flightbooking.model.BookingStatus;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class BookingRepository {

    private static final String TIME_FMT = "YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"";

    private static final String SELECT_BOOKING = """
        SELECT code,
               status,
               flight_id,
               contact_email,
               contact_phone,
               total_amount,
               total_currency,
               to_char(created_at AT TIME ZONE 'UTC', '%s') AS created_at
          FROM bookings
        """.formatted(TIME_FMT);

    private static final RowMapper<Booking> ROW_MAPPER = (rs, rowNum) -> new Booking(
            rs.getString("code"),
            BookingStatus.fromDb(rs.getString("status")),
            rs.getString("flight_id"),
            rs.getString("contact_email"),
            rs.getString("contact_phone"),
            rs.getInt("total_amount"),
            rs.getString("total_currency"),
            rs.getString("created_at")
    );

    private final JdbcTemplate jdbc;

    public BookingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Booking> findByCode(String code) {
        return jdbc.query(SELECT_BOOKING + " WHERE code = ?", ROW_MAPPER, code)
                .stream().findFirst();
    }

    public Optional<Booking> findByCodeAndLastName(String code, String lastName) {
        if (lastName == null || lastName.isBlank()) {
            return Optional.empty();
        }

        return jdbc.query(SELECT_BOOKING + """
        WHERE code = ?
          AND EXISTS (
              SELECT 1 FROM booking_passengers p
               WHERE p.booking_code = bookings.code
                 AND lower(p.last_name) = lower(?)
          )
        """, ROW_MAPPER, code, lastName)
                .stream().findFirst();
    }

    public boolean existsByCode(String code) {
        var count = jdbc.queryForObject(
                "SELECT count(*) FROM bookings WHERE code = ?",
                Integer.class, code);
        return count != null && count > 0;
    }

    public void insert(Booking booking) {
        jdbc.update("""
            INSERT INTO bookings
                (code, status, flight_id, contact_email, contact_phone,
                 total_amount, total_currency)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
                booking.code(),
                booking.status().toDb(),
                booking.flightId(),
                booking.contactEmail(),
                booking.contactPhone(),
                booking.totalAmount(),
                booking.totalCurrency());
    }

    public int updateStatus(String code, BookingStatus status) {
        return jdbc.update(
                "UPDATE bookings SET status = ? WHERE code = ?",
                status.toDb(), code);
    }
}
