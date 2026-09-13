package io.hexlet.flightbooking.controller;

import io.hexlet.flightbooking.dto.BookingCreateDto;
import io.hexlet.flightbooking.dto.BookingDto;
import io.hexlet.flightbooking.dto.LastNameBody;
import io.hexlet.flightbooking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<BookingDto> create(
            @RequestBody @Valid BookingCreateDto request) {

        var booking = bookingService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    }

    @GetMapping("/{code}")
    public BookingDto getByCode(
            @PathVariable String code,
            @RequestParam(required = false) String lastName) {

        return bookingService.findByCodeAndLastName(code, lastName);
    }

    @PostMapping("/{code}/cancel")
    public BookingDto cancel(
            @PathVariable String code,
            @RequestBody(required = false) LastNameBody body) {

        var lastName = body == null ? null : body.lastName();
        return bookingService.cancel(code, lastName);
    }
}
