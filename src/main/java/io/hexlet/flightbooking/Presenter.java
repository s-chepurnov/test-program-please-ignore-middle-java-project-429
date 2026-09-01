package io.hexlet.flightbooking;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Сборка ответов по контракту из строк базы.
 *
 * <p>Здесь живут три решения, которые в java легко нарушить незаметно.
 *
 * <ol>
 *   <li><b>Цена — {@code integer} в базе, а не {@code numeric}.</b> Драйвер отдаёт numeric как
 *       {@code BigDecimal}, и Jackson пишет его с масштабом: {@code 5400.00} вместо {@code 5400}.
 *   <li><b>Время приходит из базы строкой</b> (to_char в репозитории). Отдай {@code Instant} — и
 *       Jackson без {@code JavaTimeModule} запишет его числом epoch-секунд, а {@code LocalDate} —
 *       массивом {@code [1990,5,20]}. Оба формата контракт нарушают молча.
 *   <li><b>Ответы собираются в {@code Map}, а не в записи с полями.</b> Имена полей контракта —
 *       camelCase, и Map позволяет не заводить DTO под каждый ответ и не настраивать стратегию
 *       именования. {@code LinkedHashMap} — чтобы порядок ключей в JSON был предсказуемым при
 *       отладке.
 * </ol>
 */
public final class Presenter {

    private Presenter() {}

    public static Map<String, Object> money(int amount, String currency) {
        var money = new LinkedHashMap<String, Object>();
        money.put("amount", amount);
        money.put("currency", currency);

        return money;
    }

    public static Map<String, Object> city(String code, String name, String country) {
        var city = new LinkedHashMap<String, Object>();
        city.put("code", code);
        city.put("name", name);
        if (country != null) {
            city.put("country", country);
        }

        return city;
    }

    public static Map<String, Object> city(ResultSet rows) throws SQLException {
        return city(rows.getString("code"), rows.getString("name"), rows.getString("country"));
    }

    public static Map<String, Object> flight(ResultSet rows) throws SQLException {
        var airline = new LinkedHashMap<String, Object>();
        airline.put("code", rows.getString("airline_code"));
        airline.put("name", rows.getString("airline_name"));

        var flight = new LinkedHashMap<String, Object>();
        flight.put("id", rows.getString("id"));
        flight.put("flightNumber", rows.getString("flight_number"));
        flight.put("airline", airline);
        flight.put(
                "origin",
                city(
                        rows.getString("origin_code"),
                        rows.getString("origin_name"),
                        rows.getString("origin_country")));
        flight.put(
                "destination",
                city(
                        rows.getString("destination_code"),
                        rows.getString("destination_name"),
                        rows.getString("destination_country")));
        flight.put("departureAt", rows.getString("departure_at"));
        flight.put("arrivalAt", rows.getString("arrival_at"));
        flight.put("durationMinutes", rows.getInt("duration_minutes"));
        flight.put("price", money(rows.getInt("price_amount"), rows.getString("price_currency")));
        flight.put("seatsAvailable", rows.getInt("seats_available"));

        return flight;
    }

    public static Map<String, Object> passenger(ResultSet rows) throws SQLException {
        var passenger = new LinkedHashMap<String, Object>();
        passenger.put("firstName", rows.getString("first_name"));
        passenger.put("lastName", rows.getString("last_name"));
        passenger.put("dateOfBirth", rows.getString("date_of_birth"));
        passenger.put("documentNumber", rows.getString("document_number"));

        return passenger;
    }

    public static Map<String, Object> booking(
            ResultSet bookingRow, Map<String, Object> flight, List<Map<String, Object>> passengers)
            throws SQLException {
        var contact = new LinkedHashMap<String, Object>();
        contact.put("email", bookingRow.getString("contact_email"));
        contact.put("phone", bookingRow.getString("contact_phone"));

        var booking = new LinkedHashMap<String, Object>();
        booking.put("code", bookingRow.getString("code"));
        booking.put("status", bookingRow.getString("status"));
        booking.put("flight", flight);
        booking.put("passengers", passengers);
        booking.put("contact", contact);
        booking.put(
                "totalPrice",
                money(bookingRow.getInt("total_amount"), bookingRow.getString("total_currency")));
        booking.put("createdAt", bookingRow.getString("created_at"));

        return booking;
    }
}
