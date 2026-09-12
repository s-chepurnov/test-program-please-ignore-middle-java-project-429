package io.hexlet.flightbooking.controller;

import io.hexlet.flightbooking.dto.CityDto;
import io.hexlet.flightbooking.service.CityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cities")
public class CityController {

    private final CityService cityService;

    public CityController(CityService cityService) {
        this.cityService = cityService;
    }

    @GetMapping
    public List<CityDto> list() {

        return cityService.findAll();
    }
}
