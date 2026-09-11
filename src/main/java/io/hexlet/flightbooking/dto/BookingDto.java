package io.hexlet.flightbooking.dto;

import java.time.Instant;
import java.util.List;

public record BookingDto(
        String code,
        BookingStatus status,
        FlightDto flight,
        List<PassengerDto> passengers,
        ContactDto contact,
        MoneyDto totalPrice,
        Instant createdAt
) {}
