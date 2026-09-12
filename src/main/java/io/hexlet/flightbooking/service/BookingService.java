package io.hexlet.flightbooking.service;

import io.hexlet.flightbooking.dto.AirlineDto;
import io.hexlet.flightbooking.dto.BookingDto;
import io.hexlet.flightbooking.dto.CityDto;
import io.hexlet.flightbooking.dto.ContactDto;
import io.hexlet.flightbooking.dto.FlightDto;
import io.hexlet.flightbooking.dto.MoneyDto;
import io.hexlet.flightbooking.dto.PassengerDto;
import io.hexlet.flightbooking.model.Booking;
import io.hexlet.flightbooking.repository.AirlineRepository;
import io.hexlet.flightbooking.repository.BookingPassengerRepository;
import io.hexlet.flightbooking.repository.BookingRepository;
import io.hexlet.flightbooking.repository.CityRepository;
import io.hexlet.flightbooking.repository.FlightRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BookingService {

    private final BookingRepository bookings;
    private final BookingPassengerRepository passengers;
    private final FlightRepository flights;
    private final AirlineRepository airlines;
    private final CityRepository cities;

    public BookingService(BookingRepository bookings, BookingPassengerRepository passengers, FlightRepository flights, AirlineRepository airlines, CityRepository cities) {
        this.bookings = bookings;
        this.passengers = passengers;
        this.flights = flights;
        this.airlines = airlines;
        this.cities = cities;
    }

    public Optional<BookingDto> findByCodeAndLastName(String code, String lastName) {
        return bookings.findByCodeAndLastName(code, lastName)
                .map(this::assemble);
    }

    private BookingDto assemble(Booking b) {
        var flight = flights.findById(b.flightId()).orElseThrow();
        var airline = airlines.findByCode(flight.airlineCode()).orElseThrow();
        var origin = cities.findByCode(flight.originCode()).orElseThrow();
        var dest = cities.findByCode(flight.destinationCode()).orElseThrow();

        var passengerDtos = passengers.findByBookingCode(b.code()).stream()
                .map(p -> new PassengerDto(
                        p.firstName(), p.lastName(),
                        p.dateOfBirth(), p.documentNumber()))
                .toList();

        return new BookingDto(
                b.code(),
                b.status(),
                new FlightDto(
                        flight.id(), flight.flightNumber(),
                        new AirlineDto(airline.code(), airline.name()),
                        new CityDto(origin.code(), origin.name(), origin.country()),
                        new CityDto(dest.code(), dest.name(), dest.country()),
                        flight.departureAt(), flight.arrivalAt(),
                        flight.durationMinutes(),
                        new MoneyDto(flight.priceAmount(), flight.priceCurrency()),
                        flight.seatsAvailable()),
                passengerDtos,
                new ContactDto(b.contactEmail(), b.contactPhone()),
                new MoneyDto(b.totalAmount(), b.totalCurrency()),
                b.createdAt());
    }
}
