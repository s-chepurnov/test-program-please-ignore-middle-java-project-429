package io.hexlet.flightbooking.dto;

public record PassengerDto(
        String firstName,
        String lastName,
        String dateOfBirth,
        String documentNumber
) {}
