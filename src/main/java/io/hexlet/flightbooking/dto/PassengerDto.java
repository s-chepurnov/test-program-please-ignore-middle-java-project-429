package io.hexlet.flightbooking.dto;

import java.time.LocalDate;

public record PassengerDto(
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String documentNumber
) {}
