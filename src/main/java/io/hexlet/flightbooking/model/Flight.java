package io.hexlet.flightbooking.model;

public record Flight(
        String id,
        String flightNumber,
        String airlineCode,
        String originCode,
        String destinationCode,
        String departureAt,
        String arrivalAt,
        int durationMinutes,
        int priceAmount,
        String priceCurrency,
        int seatsAvailable) {}
