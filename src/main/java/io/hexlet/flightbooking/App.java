package io.hexlet.flightbooking;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.staticfiles.Location;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

/**
 * Приложение: маршруты API плюс раздача фронтенда с SPA-fallback.
 *
 * <p>Фреймворк здесь — деталь эталона, а не задания: студент выбирает свой. Контракт запуска
 * (переменные {@code PORT} и {@code DATABASE_URL}, цели Makefile) и контракт API от выбора не
 * зависят.
 *
 * <p>SPA-fallback живёт в обработчике «страница не найдена», а не в catch-all маршруте: у Javalin
 * статика подключается до маршрутизации, а обработчик 404 срабатывает уже после того, как ни
 * статика, ни маршруты ничего не нашли. Там же развилка — путь внутри {@code /api} отдаёт JSON-404,
 * остальное отдаёт {@code index.html}.
 */
public final class App {

    private static final int DEFAULT_PORT = 8080;
    private static final Path PUBLIC_DIR = Path.of("public");

    private App() {}

    public static void main(String[] args) {
        var dataSource = Database.dataSource();
        var app = create(dataSource);

        // Порт читает приложение, а не обёртка: контракт запуска требует переменную PORT, и хостинг
        // задаёт именно её.
        app.start("0.0.0.0", port());
    }

    static int port() {
        var raw = System.getenv("PORT");

        return raw == null || raw.isBlank() ? DEFAULT_PORT : Integer.parseInt(raw.trim());
    }

    public static Javalin create(DataSource dataSource) {
        var repository = new Repository(dataSource);

        var app =
                Javalin.create(
                        config -> {
                            if (Files.isDirectory(PUBLIC_DIR)) {
                                config.staticFiles.add(
                                        staticFiles -> {
                                            staticFiles.directory = PUBLIC_DIR.toString();
                                            staticFiles.location = Location.EXTERNAL;
                                        });
                            }

                            config.routes.get(
                                    "/api/cities", context -> context.json(repository.cities()));

                            config.routes.get(
                                    "/api/flights",
                                    context -> {
                                        var params =
                                                Validator.searchQuery(singleValueQuery(context));

                                        // Пустой результат — не ошибка: 200 и []. В том числе когда
                                        // города совпали.
                                        context.json(
                                                repository.searchFlights(
                                                        params.origin(),
                                                        params.destination(),
                                                        params.date(),
                                                        params.passengers()));
                                    });

                            config.routes.get(
                                    "/api/flights/{id}",
                                    context -> {
                                        var flight = repository.findFlight(context.pathParam("id"));
                                        if (flight.isEmpty()) {
                                            throw new ApiException(
                                                    HttpStatus.NOT_FOUND,
                                                    "not_found",
                                                    "Рейс не найден");
                                        }

                                        context.json(flight.get());
                                    });

                            config.routes.post(
                                    "/api/bookings", context -> createBooking(repository, context));

                            config.routes.get(
                                    "/api/bookings/{code}",
                                    context ->
                                            context.json(
                                                    lookup(
                                                            repository,
                                                            context.pathParam("code"),
                                                            context.queryParam("lastName"))));

                            config.routes.post(
                                    "/api/bookings/{code}/cancel",
                                    context -> cancelBooking(repository, context));

                            config.routes.error(
                                    HttpStatus.NOT_FOUND,
                                    context -> {
                                        if (context.path().startsWith("/api/")) {
                                            context.json(error("not_found", "Resource not found"));

                                            return;
                                        }

                                        var index = PUBLIC_DIR.resolve("index.html");
                                        if (!Files.isRegularFile(index)) {
                                            context.json(
                                                    error(
                                                            "not_found",
                                                            "Фронтенд не собран: нет public/index.html"));

                                            return;
                                        }

                                        try {
                                            context.contentType("text/html")
                                                    .result(Files.readString(index));
                                            context.status(HttpStatus.OK);
                                        } catch (java.io.IOException failure) {
                                            throw new IllegalStateException(failure);
                                        }
                                    });

                            // Формат ошибки задаём сами: у Javalin по умолчанию тело пустое или
                            // текстовое, а контракт
                            // требует {code, message}.
                            config.routes.exception(
                                    Validator.ValidationException.class,
                                    (failure, context) ->
                                            context.status(HttpStatus.BAD_REQUEST)
                                                    .json(
                                                            error(
                                                                    "validation_error",
                                                                    failure.getMessage())));

                            config.routes.exception(
                                    ApiException.class,
                                    (failure, context) ->
                                            context.status(failure.status())
                                                    .json(
                                                            error(
                                                                    failure.code(),
                                                                    failure.getMessage())));

                            config.routes.exception(
                                    Exception.class,
                                    (failure, context) -> {
                                        // Деталей клиенту не отдаём, но в лог они попадают: без
                                        // этого причина 500 не
                                        // видна ни в docker logs, ни в логе CI.
                                        failure.printStackTrace();
                                        context.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                .json(
                                                        error(
                                                                "internal_error",
                                                                "Внутренняя ошибка сервера"));
                                    });
                        });

        return app;
    }

    private static void createBooking(Repository repository, Context context) throws SQLException {
        var payload = Validator.createBooking(context.bodyAsClass(Map.class));

        // Неизвестный рейс — ошибка запроса, а не отсутствующий ресурс: по контракту 400.
        if (!repository.flightExists(payload.flightId())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "validation_error",
                    "Неизвестный flightId: " + payload.flightId());
        }

        var code =
                repository.createBooking(
                        payload.flightId(), payload.contact(), payload.passengers());
        var booking = repository.findBooking(code, payload.passengers().get(0).lastName());

        context.status(HttpStatus.CREATED).json(booking.orElseThrow());
    }

    private static void cancelBooking(Repository repository, Context context) throws SQLException {
        // Фамилия приходит в теле, а не в query: у отмены есть тело запроса, и второй фактор
        // доступа
        // к чужой брони незачем светить в адресе, логах и истории браузера.
        var body =
                context.body().isBlank()
                        ? Map.<String, Object>of()
                        : context.bodyAsClass(Map.class);
        var lastName = body.get("lastName");
        var booking =
                lookup(
                        repository,
                        context.pathParam("code"),
                        lastName == null ? null : String.valueOf(lastName));

        // Бронь не удаляется, у неё меняется статус. Повторная отмена не ломается.
        repository.cancelBooking(String.valueOf(booking.get("code")));

        var cancelled = new LinkedHashMap<>(booking);
        cancelled.put("status", "cancelled");

        context.json(cancelled);
    }

    /**
     * Просмотр и отмена используют одну пару факторов: код и фамилию. Отсутствующая фамилия даёт
     * тот же 404, что и неверная: иначе перебором узнаются живые коды.
     */
    private static Map<String, Object> lookup(Repository repository, String code, String lastName)
            throws SQLException {
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "not_found", "Бронь не найдена");
        }

        return repository
                .findBooking(code, lastName)
                .orElseThrow(
                        () ->
                                new ApiException(
                                        HttpStatus.NOT_FOUND, "not_found", "Бронь не найдена"));
    }

    /**
     * Javalin отдаёт значения query-параметров списками (у параметра может быть несколько
     * значений). Валидатору нужен плоский вид, поэтому берём первое значение каждого.
     */
    private static Map<String, String> singleValueQuery(Context context) {
        var flat = new LinkedHashMap<String, String>();
        for (var entry : context.queryParamMap().entrySet()) {
            List<String> values = entry.getValue();
            flat.put(entry.getKey(), values.isEmpty() ? null : values.get(0));
        }

        return flat;
    }

    private static Map<String, Object> error(String code, String message) {
        var body = new LinkedHashMap<String, Object>();
        body.put("code", code);
        body.put("message", message);

        return body;
    }
}
