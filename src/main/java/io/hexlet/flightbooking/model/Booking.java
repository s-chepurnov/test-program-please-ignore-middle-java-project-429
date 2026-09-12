package io.hexlet.flightbooking.model;

public record Booking(
        String code,
        BookingStatus status,
        String flightId,
        String contactEmail,
        String contactPhone,
        int totalAmount,
        String totalCurrency,
        String createdAt) {}
