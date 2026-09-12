package io.hexlet.flightbooking.dto;

public record FlightDto(
        String id,
        String flightNumber,
        AirlineDto airline,
        CityDto origin,
        CityDto destination,
        String departureAt,
        String arrivalAt,
        int durationMinutes,
        MoneyDto price,
        int seatsAvailable
) {}
