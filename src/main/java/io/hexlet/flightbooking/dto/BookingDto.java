package io.hexlet.flightbooking.dto;

import io.hexlet.flightbooking.model.BookingStatus;

import java.util.List;

public record BookingDto(
        String code,
        BookingStatus status,
        FlightDto flight,
        List<PassengerDto> passengers,
        ContactDto contact,
        MoneyDto totalPrice,
        String createdAt
) {}
