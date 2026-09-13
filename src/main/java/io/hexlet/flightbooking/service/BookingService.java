package io.hexlet.flightbooking.service;

import io.hexlet.flightbooking.dto.BookingCreateDto;
import io.hexlet.flightbooking.dto.BookingDto;
import io.hexlet.flightbooking.dto.ContactDto;
import io.hexlet.flightbooking.dto.FlightDto;
import io.hexlet.flightbooking.dto.MoneyDto;
import io.hexlet.flightbooking.dto.PassengerDto;
import io.hexlet.flightbooking.exception.BadRequestException;
import io.hexlet.flightbooking.exception.NotFoundException;
import io.hexlet.flightbooking.model.Booking;
import io.hexlet.flightbooking.model.BookingPassenger;
import io.hexlet.flightbooking.model.BookingStatus;
import io.hexlet.flightbooking.repository.BookingPassengerRepository;
import io.hexlet.flightbooking.repository.BookingRepository;
import io.hexlet.flightbooking.repository.FlightRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private static final String NOT_FOUND_MESSAGE = "Бронь не найдена";

    private static final int CODE_ATTEMPTS = 5;

    private final FlightRepository flights;
    private final BookingRepository bookings;
    private final BookingPassengerRepository passengers;
    private final BookingCodeGenerator codes;

    public BookingService(FlightRepository flights,
                          BookingRepository bookings,
                          BookingPassengerRepository passengers,
                          BookingCodeGenerator codes) {
        this.flights = flights;
        this.bookings = bookings;
        this.passengers = passengers;
        this.codes = codes;
    }

    @Transactional
    public BookingDto create(BookingCreateDto request) {
        var flight = flights.findById(request.flightId())
                .orElseThrow(() -> new BadRequestException(
                        "Рейс с id '%s' не найден".formatted(request.flightId())));

        var totalAmount = flight.price().amount() * request.passengers().size();
        var code = generateUniqueCode();

        var booking = new Booking(
                code,
                BookingStatus.CONFIRMED,
                flight.id(),
                request.contact().email(),
                request.contact().phone(),
                totalAmount,
                flight.price().currency(),
                null);
        bookings.insert(booking);
        passengers.insertAll(code, toPassengerModels(request.passengers()));

        var saved = bookings.findByCode(code)
                .orElseThrow(() -> new IllegalStateException(
                        "Только что вставленная бронь не найдена: " + code));

        return assemble(saved, flight, toPassengerDtos(saved.code()));
    }

    public BookingDto findByCodeAndLastName(String code, String lastName) {
        var normalizedCode = normalizeCode(code);
        var normalizedLastName = normalizeLastName(lastName);

        // Отсутствие фамилии — не 400, а тот же 404, что и «фамилия не та».
        // Иначе по разнице статусов можно было бы отличить запрос без фамилии
        // от запроса с неверной фамилией, а это уже утечка.
        if (normalizedCode.isEmpty() || normalizedLastName.isEmpty()) {
            throw new NotFoundException(NOT_FOUND_MESSAGE);
        }

        var booking = bookings.findByCodeAndLastName(normalizedCode, normalizedLastName)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));

        var flight = flights.findById(booking.flightId())
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));

        return assemble(booking, flight, toPassengerDtos(booking.code()));
    }

    private static String normalizeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeLastName(String lastName) {
        return lastName == null ? "" : lastName.trim();
    }

    private String generateUniqueCode() {
        for (var attempt = 0; attempt < CODE_ATTEMPTS; attempt++) {
            var code = codes.generate();
            if (!bookings.existsByCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException(
                "Не удалось сгенерировать уникальный код за %d попыток".formatted(CODE_ATTEMPTS));
    }

    private List<BookingPassenger> toPassengerModels(List<PassengerDto> dtos) {
        var result = new ArrayList<BookingPassenger>(dtos.size());
        for (var i = 0; i < dtos.size(); i++) {
            var p = dtos.get(i);
            result.add(new BookingPassenger(
                    null,
                    null,
                    i + 1,
                    p.firstName(),
                    p.lastName(),
                    p.dateOfBirth(),
                    p.documentNumber()));
        }
        return result;
    }

    private List<PassengerDto> toPassengerDtos(String bookingCode) {
        return passengers.findByBookingCode(bookingCode).stream()
                .map(p -> new PassengerDto(
                        p.firstName(),
                        p.lastName(),
                        p.dateOfBirth(),
                        p.documentNumber()))
                .toList();
    }

    private BookingDto assemble(Booking b, FlightDto flight, List<PassengerDto> passengers) {
        return new BookingDto(
                b.code(),
                b.status(),
                flight,
                passengers,
                new ContactDto(b.contactEmail(), b.contactPhone()),
                new MoneyDto(b.totalAmount(), b.totalCurrency()),
                b.createdAt());
    }

    @Transactional
    public BookingDto cancel(String code, String lastName) {
        var normalizedCode = normalizeCode(code);
        var normalizedLastName = normalizeLastName(lastName);

        if (normalizedCode.isEmpty() || normalizedLastName.isEmpty()) {
            throw new NotFoundException(NOT_FOUND_MESSAGE);
        }

        var booking = bookings.findByCodeAndLastName(normalizedCode, normalizedLastName)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));

        if (booking.status() != BookingStatus.CANCELLED) {
            bookings.updateStatus(booking.code(), BookingStatus.CANCELLED);
        }

        var updated = bookings.findByCode(booking.code())
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));

        var flight = flights.findById(updated.flightId())
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));

        return assemble(updated, flight, toPassengerDtos(updated.code()));
    }
}
