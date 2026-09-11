package io.hexlet.flightbooking.controller;

import io.hexlet.flightbooking.dto.HealthDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public HealthDto check() {
        // Spring Boot сам вернет статус 200 OK и превратит объект в {"status": "ok"}
        return new HealthDto("ok");
    }
}
