package io.hexlet.flightbooking.repository;

import io.hexlet.flightbooking.model.BookingPassenger;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class BookingPassengerRepository {

    private static final RowMapper<BookingPassenger> ROW_MAPPER = (rs, rowNum) ->
            new BookingPassenger(
                    rs.getObject("id", Long.class),
                    rs.getString("booking_code"),
                    rs.getInt("position"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("date_of_birth"),
                    rs.getString("document_number")
            );

    private final JdbcTemplate jdbc;

    public BookingPassengerRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<BookingPassenger> findByBookingCode(String bookingCode) {
        return jdbc.query("""
            SELECT id, booking_code, position, first_name, last_name,
                   date_of_birth, document_number
              FROM booking_passengers
             WHERE booking_code = ?
             ORDER BY position
            """, ROW_MAPPER, bookingCode);
    }

    public void insertAll(String bookingCode, List<BookingPassenger> passengers) {
        jdbc.batchUpdate("""
            INSERT INTO booking_passengers
                (booking_code, position, first_name, last_name,
                 date_of_birth, document_number)
            VALUES (?, ?, ?, ?, ?::date, ?)
            """,
                passengers,
                passengers.size(),
                (ps, p) -> {
                    ps.setString(1, bookingCode);
                    ps.setInt(2, p.position());
                    ps.setString(3, p.firstName());
                    ps.setString(4, p.lastName());
                    ps.setString(5, p.dateOfBirth());     // "1987-12-01"
                    ps.setString(6, p.documentNumber());
                });
    }

    public boolean existsByBookingCodeAndLastName(String bookingCode, String lastName) {
        if (lastName == null || lastName.isBlank()) {
            return false;
        }

        var count = jdbc.queryForObject("""
            SELECT count(*) FROM booking_passengers
             WHERE booking_code = ?
               AND lower(last_name) = lower(?)
            """, Integer.class, bookingCode, lastName);
        return count != null && count > 0;
    }
}
