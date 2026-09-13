package io.hexlet.flightbooking.service;

import io.hexlet.flightbooking.dto.FlightDto;
import io.hexlet.flightbooking.exception.NotFoundException;
import io.hexlet.flightbooking.repository.FlightRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FlightService {

    private final FlightRepository flights;

    public FlightService(FlightRepository flights) {
        this.flights = flights;
    }

    public List<FlightDto> search(String origin, String destination,
                                  LocalDate date, int passengers) {
        return flights.search(origin, destination, date, passengers);
    }

    public FlightDto getById(String id) {
        return flights.findById(id)
                .orElseThrow(() -> new NotFoundException("Рейс с таким id не найден"));
    }
}
