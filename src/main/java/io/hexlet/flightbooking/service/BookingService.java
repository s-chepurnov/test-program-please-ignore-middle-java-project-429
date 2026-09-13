package io.hexlet.flightbooking.service;

import io.hexlet.flightbooking.repository.AirlineRepository;
import io.hexlet.flightbooking.repository.BookingPassengerRepository;
import io.hexlet.flightbooking.repository.BookingRepository;
import io.hexlet.flightbooking.repository.CityRepository;
import io.hexlet.flightbooking.repository.FlightRepository;
import org.springframework.stereotype.Service;

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




}
