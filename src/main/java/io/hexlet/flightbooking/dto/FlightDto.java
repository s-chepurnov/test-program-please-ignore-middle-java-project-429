package io.hexlet.flightbooking.dto;

import java.time.Instant;

public record FlightDto(
        String id,
        String flightNumber,
        AirlineDto airline,
        CityDto origin,
        CityDto destination,
        Instant departureAt,
        Instant arrivalAt,
        int durationMinutes,
        MoneyDto price,
        int seatsAvailable
) {}
