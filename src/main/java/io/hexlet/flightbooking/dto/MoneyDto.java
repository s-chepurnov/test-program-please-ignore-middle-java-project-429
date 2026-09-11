package io.hexlet.flightbooking.dto;

public record MoneyDto(
        int amount,
        String currency
) {
    public MoneyDto(int amount) {
        this(amount, "RUB");
    }
}
