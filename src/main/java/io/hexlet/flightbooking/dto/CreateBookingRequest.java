package io.hexlet.flightbooking.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateBookingRequest(
        @NotNull
        String flightId,

        @NotNull
        ContactDto contact,

        @NotEmpty(message = "Список пассажиров должен содержать минимум одного человека")
        List<PassengerDto> passengers
) {}
