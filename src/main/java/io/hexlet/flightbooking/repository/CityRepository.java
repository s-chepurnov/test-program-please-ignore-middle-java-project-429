package io.hexlet.flightbooking.repository;

import io.hexlet.flightbooking.model.City;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class CityRepository {

    private static final RowMapper<City> ROW_MAPPER = (rs, rowNum) -> new City(
            rs.getString("code"),
            rs.getString("name"),
            rs.getString("country"),
            rs.getInt("sort_order")
    );

    private final JdbcTemplate jdbc;

    public CityRepository(JdbcTemplate jdbc) throws SQLException {
        this.jdbc = jdbc;
    }

    public List<City> findAll() {
        return jdbc.query(
                "SELECT code, name, country, sort_order FROM cities ORDER BY sort_order",
                ROW_MAPPER);
    }

    public Optional<City> findByCode(String code) {
        return jdbc.query(
                        "SELECT code, name, country, sort_order FROM cities WHERE code = ?",
                        ROW_MAPPER, code)
                .stream().findFirst();
    }
}
