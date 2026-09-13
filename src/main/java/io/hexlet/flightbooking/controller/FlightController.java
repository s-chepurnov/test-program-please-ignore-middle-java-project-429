package io.hexlet.flightbooking.controller;

import io.hexlet.flightbooking.dto.FlightDto;
import io.hexlet.flightbooking.service.FlightService;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/flights")
@Validated
public class FlightController {

    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    @GetMapping
    public List<FlightDto> search(
            @RequestParam @NotBlank(message = "должен быть указан") String origin,
            @RequestParam @NotBlank(message = "должен быть указан") String destination,
            @RequestParam @NotNull(message = "должен быть указан")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "должен быть не меньше 1") int passengers) {

        return flightService.search(origin, destination, date, passengers);
    }

    @GetMapping("/{id}")
    public FlightDto getById(@PathVariable String id) {
        return flightService.getById(id);
    }
}
