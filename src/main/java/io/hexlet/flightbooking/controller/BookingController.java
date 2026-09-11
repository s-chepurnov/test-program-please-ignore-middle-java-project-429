package io.hexlet.flightbooking.controller;

import io.hexlet.flightbooking.dto.BookingDto;
import io.hexlet.flightbooking.dto.CreateBookingRequest;
import io.hexlet.flightbooking.dto.LastNameBody;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingDto create(@Valid @RequestBody CreateBookingRequest body) {

        return null;
    }

    @PostMapping("/{code}/verify")
    public BookingDto verify(@PathVariable String code, @RequestBody LastNameBody body) {

        return null;
    }

    @PostMapping("/{code}/cancel")
    public BookingDto cancel(@PathVariable String code, @RequestBody LastNameBody body) {

        return null;
    }
}
