package io.hexlet.flightbooking.repository;

import io.hexlet.flightbooking.dto.AirlineDto;
import io.hexlet.flightbooking.dto.CityDto;
import io.hexlet.flightbooking.dto.FlightDto;
import io.hexlet.flightbooking.dto.MoneyDto;
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
        SELECT f.id,
               f.flight_number,
               f.duration_minutes,
               f.price_amount,
               f.price_currency,
               f.seats_available,
               to_char(f.departure_at AT TIME ZONE 'UTC', '%s') AS departure_at,
               to_char(f.arrival_at   AT TIME ZONE 'UTC', '%s') AS arrival_at,
               a.code       AS airline_code,
               a.name       AS airline_name,
               oc.code      AS origin_code,
               oc.name      AS origin_name,
               oc.country   AS origin_country,
               dc.code      AS dest_code,
               dc.name      AS dest_name,
               dc.country   AS dest_country
          FROM flights f
          JOIN airlines a  ON a.code  = f.airline_code
          JOIN cities   oc ON oc.code = f.origin_code
          JOIN cities   dc ON dc.code = f.destination_code
        """.formatted(TIME_FMT, TIME_FMT);

    private static final RowMapper<FlightDto> FLIGHT_DTO_MAPPER = (rs, rowNum) -> new FlightDto(
            rs.getString("id"),
            rs.getString("flight_number"),
            new AirlineDto(rs.getString("airline_code"), rs.getString("airline_name")),
            new CityDto(rs.getString("origin_code"), rs.getString("origin_name"),
                    rs.getString("origin_country")),
            new CityDto(rs.getString("dest_code"), rs.getString("dest_name"),
                    rs.getString("dest_country")),
            rs.getString("departure_at"),
            rs.getString("arrival_at"),
            rs.getInt("duration_minutes"),
            new MoneyDto(rs.getInt("price_amount"), rs.getString("price_currency")),
            rs.getInt("seats_available")
    );

    private final JdbcTemplate jdbc;

    public FlightRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // строго говоря, DTO не должно быть в репозитории!
    // но учитывается подсказка на шаге 6:
    // "Города в ответе — объекты целиком, а не коды.
    // Это соединение с таблицей городов; собирать его запросом на каждый рейс отдельно не стоит."
    public List<FlightDto> search(String origin, String destination,
                                  LocalDate date, int passengers) {
        return jdbc.query(SELECT_FLIGHT + """
            WHERE f.origin_code = ?
              AND f.destination_code = ?
              AND (f.departure_at AT TIME ZONE 'UTC')::date = ?::date
              AND f.seats_available >= ?
            ORDER BY f.departure_at
            """, FLIGHT_DTO_MAPPER, origin, destination, date, passengers);
    }

    public Optional<FlightDto> findById(String id) {
        return jdbc.query(SELECT_FLIGHT + " WHERE f.id = ?", FLIGHT_DTO_MAPPER, id)
                .stream().findFirst();
    }
}
