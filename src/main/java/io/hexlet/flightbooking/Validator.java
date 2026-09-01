package io.hexlet.flightbooking;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Проверяем наличие и непустоту обязательных полей, а не форматы.
 *
 * <p>Это осознанное решение: клиент присылает, например, номер документа «1» — такое значение
 * валидное. Регулярное выражение на серию и номер паспорта, на телефон или строгая проверка email
 * ломают рабочий сценарий и делают API непригодным для фронтенда.
 */
public final class Validator {

    private Validator() {}

    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }

    public record Contact(String email, String phone) {}

    public record Passenger(
            String firstName, String lastName, String dateOfBirth, String documentNumber) {}

    public record SearchQuery(String origin, String destination, String date, int passengers) {}

    public record BookingRequest(String flightId, Contact contact, List<Passenger> passengers) {}

    public static SearchQuery searchQuery(Map<String, String> query) {
        for (var field : List.of("origin", "destination", "date")) {
            if (blank(query.get(field))) {
                throw new ValidationException("Укажите параметр " + field);
            }
        }

        var date = parseDate(query.get("date"));
        if (date == null) {
            throw new ValidationException("Параметр date должен быть датой в формате YYYY-MM-DD");
        }

        var passengers = 1;
        var raw = query.get("passengers");
        if (!blank(raw)) {
            try {
                passengers = Integer.parseInt(raw.trim());
            } catch (NumberFormatException error) {
                throw new ValidationException(
                        "Параметр passengers должен быть целым числом не меньше 1");
            }

            if (passengers < 1) {
                throw new ValidationException(
                        "Параметр passengers должен быть целым числом не меньше 1");
            }
        }

        return new SearchQuery(
                query.get("origin").trim(), query.get("destination").trim(), date, passengers);
    }

    public static BookingRequest createBooking(Map<String, Object> body) {
        if (body == null) {
            throw new ValidationException("Тело запроса должно быть JSON-объектом");
        }

        if (blank(text(body.get("flightId")))) {
            throw new ValidationException("Укажите flightId");
        }

        var contact = asMap(body.get("contact"));
        if (contact == null) {
            throw new ValidationException("Укажите contact");
        }

        for (var field : List.of("email", "phone")) {
            if (blank(text(contact.get(field)))) {
                throw new ValidationException("Укажите contact." + field);
            }
        }

        if (!(body.get("passengers") instanceof List<?> rawPassengers) || rawPassengers.isEmpty()) {
            throw new ValidationException("Укажите хотя бы одного пассажира");
        }

        var passengers = new ArrayList<Passenger>();
        for (var index = 0; index < rawPassengers.size(); index++) {
            passengers.add(passenger(rawPassengers.get(index), index));
        }

        return new BookingRequest(
                text(body.get("flightId")).trim(),
                new Contact(text(contact.get("email")).trim(), text(contact.get("phone")).trim()),
                passengers);
    }

    private static Passenger passenger(Object raw, int index) {
        var item = asMap(raw);
        if (item == null) {
            throw new ValidationException("Пассажир " + index + " должен быть объектом");
        }

        for (var field : List.of("firstName", "lastName", "dateOfBirth", "documentNumber")) {
            if (blank(text(item.get(field)))) {
                throw new ValidationException("Укажите passengers[" + index + "]." + field);
            }
        }

        var dateOfBirth = parseDate(text(item.get("dateOfBirth")));
        if (dateOfBirth == null) {
            throw new ValidationException(
                    "passengers[" + index + "].dateOfBirth должна быть датой в формате YYYY-MM-DD");
        }

        return new Passenger(
                text(item.get("firstName")).trim(),
                text(item.get("lastName")).trim(),
                dateOfBirth,
                text(item.get("documentNumber")).trim());
    }

    /**
     * Дата в формате контракта. {@code LocalDate.parse} принимает только ISO-форму, поэтому
     * «03.08.2026» отсекается здесь, а не в базе.
     */
    private static String parseDate(String value) {
        if (blank(value)) {
            return null;
        }

        try {
            return LocalDate.parse(value.trim()).toString();
        } catch (DateTimeParseException error) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : null;
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
