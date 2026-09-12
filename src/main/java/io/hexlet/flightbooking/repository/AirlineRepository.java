package io.hexlet.flightbooking.repository;

import io.hexlet.flightbooking.model.Airline;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AirlineRepository {

    private static final RowMapper<Airline> ROW_MAPPER = (rs, rowNum) -> new Airline(
            rs.getString("code"),
            rs.getString("name")
    );

    private final JdbcTemplate jdbc;

    public AirlineRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Airline> findByCode(String code) {
        return jdbc.query(
                        "SELECT code, name FROM airlines WHERE code = ?",
                        ROW_MAPPER, code)
                .stream().findFirst();
    }
}
