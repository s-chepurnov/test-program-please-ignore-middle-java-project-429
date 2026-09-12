package io.hexlet.flightbooking.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BookingStatus {
    CONFIRMED,
    CANCELLED;

    /** Для JDBC-чтения: в БД лежит в нижнем регистре. */
    public static BookingStatus fromDb(String value) {
        return BookingStatus.valueOf(value.toUpperCase());
    }

    /** Для JDBC-записи. */
    public String toDb() {
        return name().toLowerCase();
    }

    /** Для Jackson. */
    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static BookingStatus fromJson(String value) {
        return value == null ? null : valueOf(value.toUpperCase());
    }
}
