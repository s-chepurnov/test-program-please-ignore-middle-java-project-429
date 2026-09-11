package io.hexlet.flightbooking.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum BookingStatus {
    @JsonProperty("confirmed")
    CONFIRMED,

    @JsonProperty("cancelled")
    CANCELLED
}
