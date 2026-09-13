package io.hexlet.flightbooking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record BookingCreateDto(
        @NotBlank(message = "должен быть указан") String flightId,
        @NotNull(message = "должен быть указан") @Valid ContactDto contact,
        @NotEmpty(message = "должен содержать хотя бы одного пассажира")
        @Valid List<PassengerDto> passengers) {}
