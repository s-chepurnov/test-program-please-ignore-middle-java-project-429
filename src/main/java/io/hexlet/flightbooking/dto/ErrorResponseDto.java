package io.hexlet.flightbooking.dto;

public record ErrorResponseDto(String code, String message) {
    public static ErrorResponseDto validationError(String message) {
        return new ErrorResponseDto("validation_error", message);
    }
    public static ErrorResponseDto notFound(String message) {
        return new ErrorResponseDto("not_found", message);
    }
}
