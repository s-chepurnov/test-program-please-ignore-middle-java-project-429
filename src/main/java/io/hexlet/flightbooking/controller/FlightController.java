package io.hexlet.flightbooking.controller;

import io.hexlet.flightbooking.dto.FlightDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/flights")
public class FlightController {

    @GetMapping
    public List<FlightDto> search(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "1") int passengers
    ) {


        return new ArrayList<>();
    }

    @GetMapping("/{id}")
    public FlightDto getById(@PathVariable String id) {


        return null;
    }
}
