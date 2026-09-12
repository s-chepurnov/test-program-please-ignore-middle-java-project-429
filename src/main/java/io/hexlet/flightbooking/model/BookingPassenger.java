package io.hexlet.flightbooking.model;

public record BookingPassenger(
        Long id,
        String bookingCode,
        int position,
        String firstName,
        String lastName,
        String dateOfBirth,
        String documentNumber) {}
