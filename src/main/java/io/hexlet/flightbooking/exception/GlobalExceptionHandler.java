package io.hexlet.flightbooking.exception;

import io.hexlet.flightbooking.dto.ErrorResponseDto;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDate;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** @NotBlank / @NotNull / @Min на @RequestParam. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleConstraintViolation(ConstraintViolationException e) {
        var message = e.getConstraintViolations().stream()
                .map(v -> {
                    var path = v.getPropertyPath().toString();
                    var field = path.contains(".")
                            ? path.substring(path.lastIndexOf('.') + 1)
                            : path;
                    return "Параметр '%s' %s".formatted(field, v.getMessage());
                })
                .findFirst()
                .orElse("Некорректные параметры запроса");

        return badRequest(message);
    }

    /** Неверный формат значения: date=31-12-2026, passengers=abc. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        var name = e.getName();
        var value = e.getValue();
        var expected = describeExpectedType(e.getRequiredType());

        var message = "Параметр '%s' имеет неверный формат: ожидается %s, получено '%s'"
                .formatted(name, expected, value);

        return badRequest(message);
    }

    /** Обязательный параметр отсутствует: /api/flights без ?date=... */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDto> handleMissingParam(MissingServletRequestParameterException e) {
        var message = "Параметр '%s' обязателен".formatted(e.getParameterName());
        return badRequest(message);
    }

    /** Рейс не найден: GET /api/flights/{id}. */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFound(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponseDto.notFound(e.getMessage()));
    }

    /** Всё остальное — чтобы клиент не получил HTML-страницу вместо JSON. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleUnexpected(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponseDto.validationError("Внутренняя ошибка сервера"));
    }

    private static String describeExpectedType(Class<?> type) {
        if (type == LocalDate.class) {
            return "дата в формате YYYY-MM-DD";
        }
        if (type == Integer.class || type == int.class) {
            return "целое число";
        }
        if (type == Long.class || type == long.class) {
            return "целое число";
        }
        return type != null ? type.getSimpleName() : "значение";
    }

    private static ResponseEntity<ErrorResponseDto> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDto.validationError(message));
    }
}
