package io.hexlet.flightbooking.service;

import io.hexlet.flightbooking.dto.CityDto;
import io.hexlet.flightbooking.model.City;
import io.hexlet.flightbooking.repository.CityRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CityService {

    private final CityRepository cities;

    public CityService(CityRepository cities) {
        this.cities = cities;
    }

    public List<CityDto> findAll() {
        return cities.findAll().stream()
                .map(CityService::toDto)
                .toList();
    }

    private static CityDto toDto(City city) {
        return new CityDto(city.code(), city.name(), city.country());
    }
}
