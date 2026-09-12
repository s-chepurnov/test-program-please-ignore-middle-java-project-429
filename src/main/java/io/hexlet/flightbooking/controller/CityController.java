package io.hexlet.flightbooking.controller;

import io.hexlet.flightbooking.dto.CityDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/cities")
public class CityController {

    @GetMapping
    public List<CityDto> list() {


        return new ArrayList<>();
    }
}
