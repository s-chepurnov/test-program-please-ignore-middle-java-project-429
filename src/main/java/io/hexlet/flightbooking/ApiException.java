package io.hexlet.flightbooking;

import io.javalin.http.HttpStatus;

/** Ошибка, которую маршрут отдаёт осознанно: статус плюс код и текст по контракту. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }
}
