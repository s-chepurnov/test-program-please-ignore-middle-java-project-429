package io.hexlet.flightbooking.model;

public record City(
        String code,
        String name,
        String country,
        int sortOrder) {}
