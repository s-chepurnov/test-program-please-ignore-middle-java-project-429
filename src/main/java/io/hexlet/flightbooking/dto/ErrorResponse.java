package io.hexlet.flightbooking.dto;

public record ErrorResponse(
        String code,
        String message
) {}
