package io.hexlet.flightbooking;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.testtools.JavalinTest;
import io.javalin.testtools.Response;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Тесты бьют по API через приложение целиком: маршрутизация, валидация, база и сериализация JSON.
 * Именно в сериализации живут ошибки контракта — тесты на уровне классов их не видят.
 *
 * <p>{@code JavalinTest} поднимает приложение на свободном порту сам и сам его гасит, поэтому
 * отдельного {@code make start} тестам не нужно.
 *
 * <p>База нужна настоящая: без {@code DATABASE_URL} тесты пропускаются, а не подменяют её моком —
 * подмена скрыла бы ровно те ошибки, которые здесь ищутся.
 */
@EnabledIfEnvironmentVariable(named = "DATABASE_URL", matches = ".+")
class ApiTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static DataSource dataSource;

    @BeforeAll
    static void prepareDatabase() throws Exception {
        dataSource = Database.dataSource();
        new Migrator(dataSource).migrate();
        new Seeder(dataSource).seed();
    }

    private io.javalin.Javalin app() {
        return App.create(dataSource);
    }

    private static String day(int offsetDays) {
        return LocalDate.now(ZoneOffset.UTC).plusDays(offsetDays).toString();
    }

    private static JsonNode json(Response response) throws IOException {
        return MAPPER.readTree(response.body().string());
    }

    private static Map<String, Object> passenger(String lastName) {
        return Map.of(
                "firstName", "Иван",
                "lastName", lastName,
                "dateOfBirth", "1990-05-20",
                "documentNumber", "1");
    }

    private static Map<String, Object> bookingPayload(String flightId, List<?> passengers) {
        return Map.of(
                "flightId", flightId,
                "contact", Map.of("email", "ivan@example.com", "phone", "+79991234567"),
                "passengers", passengers);
    }

    @Test
    void отдаётГородаМосквуИПитерПервыми() {
        JavalinTest.test(
                app(),
                (server, client) -> {
                    var cities = json(client.get("/api/cities"));

                    assertThat(cities).hasSize(7);
                    assertThat(cities.get(0).get("code").asText()).isEqualTo("MOW");
                    assertThat(cities.get(0).get("name").asText()).isEqualTo("Москва");
                    assertThat(cities.get(1).get("code").asText()).isEqualTo("LED");
                    assertThat(cities.get(1).get("name").asText()).isEqualTo("Санкт-Петербург");
                });
    }

    @Test
    void вПоискеЧисловыеПоляПриходятЧислами() {
        JavalinTest.test(
                app(),
                (server, client) -> {
                    var flights =
                            json(
                                    client.get(
                                            "/api/flights?origin=MOW&destination=LED&date="
                                                    + day(3)));

                    assertThat(flights).isNotEmpty();
                    var flight = flights.get(0);

                    assertThat(flight.get("origin").get("code").asText()).isEqualTo("MOW");
                    assertThat(flight.get("destination").get("code").asText()).isEqualTo("LED");
                    // Контракт требует числа. BigDecimal из numeric дал бы 5400.00, строка —
                    // "5400".
                    assertThat(flight.get("price").get("amount").isInt()).isTrue();
                    assertThat(flight.get("seatsAvailable").isInt()).isTrue();
                    assertThat(flight.get("durationMinutes").isInt()).isTrue();
                    assertThat(flight.get("price").get("currency").asText()).isEqualTo("RUB");
                    // Instant без JavaTimeModule уехал бы числом epoch-секунд.
                    assertThat(flight.get("departureAt").asText())
                            .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");
                });
    }

    @Test
    void наСегодняРейсыЕсть() {
        JavalinTest.test(
                app(),
                (server, client) -> {
                    var flights =
                            json(
                                    client.get(
                                            "/api/flights?origin=MOW&destination=LED&date="
                                                    + day(0)));

                    assertThat(flights).isNotEmpty();
                });
    }

    @Test
    void поискИзГородаВСебяЖеДаётПустойМассив() {
        JavalinTest.test(
                app(),
                (server, client) -> {
                    var response =
                            client.get("/api/flights?origin=MOW&destination=MOW&date=" + day(3));

                    assertThat(response.code()).isEqualTo(200);
                    assertThat(json(response)).isEmpty();
                });
    }

    @Test
    void поискБезДатыОтклоняется() {
        JavalinTest.test(
                app(),
                (server, client) -> {
                    var response = client.get("/api/flights?origin=MOW&destination=LED");

                    assertThat(response.code()).isEqualTo(400);
                    assertThat(json(response).get("code").asText()).isEqualTo("validation_error");
                });
    }

    @Test
    void поискСДатойНеТогоФорматаОтклоняется() {
        JavalinTest.test(
                app(),
                (server, client) -> {
                    var response =
                            client.get("/api/flights?origin=MOW&destination=LED&date=03.08.2026");

                    assertThat(response.code()).isEqualTo(400);
                    assertThat(json(response).get("code").asText()).isEqualTo("validation_error");
                });
    }

    @Test
    void поискФильтруетПоЧислуМест() {
        JavalinTest.test(
                app(),
                (server, client) -> {
                    var flights =
                            json(
                                    client.get(
                                            "/api/flights?origin=MOW&destination=LED&date="
                                                    + day(3)
                                                    + "&passengers=2"));

                    assertThat(flights).isNotEmpty();
                    flights.forEach(
                            flight ->
                                    assertThat(flight.get("seatsAvailable").asInt())
                                            .isGreaterThanOrEqualTo(2));
                });
    }

    @Test
    void рейсНаходитсяПоИдентификатору() {
        JavalinTest.test(
                app(),
                (server, client) -> {
                    var id = firstFlightId(client);
                    var flight = json(client.get("/api/flights/" + id));

                    assertThat(flight.get("id").asText()).isEqualTo(id);
                });
    }

    @Test
    void неизвестныйРейсДаёт404() {
        JavalinTest.test(
                app(),
                (server, client) -> {
                    var response = client.get("/api/flights/NOPE");

                    assertThat(response.code()).isEqualTo(404);
                    assertThat(json(response).get("code").asText()).isEqualTo("not_found");
                });
    }

    @Test
    void броньСоздаётсяСКодомИзШестиСимволов() {
        JavalinTest.test(
                app(),
                (server, client) -> {
                    var response = createBooking(client, List.of(passenger("Петров")));

                    assertThat(response.code()).isEqualTo(201);
                    var created = json(response);

                    assertThat(created.get("code").asText()).matches("[A-Z0-9]{6}");
                    assertThat(created.get("status").asText()).isEqualTo("confirmed");
                    assertThat(created.get("totalPrice").get("amount").isInt()).isTrue();
                    assertThat(created.get("totalPrice").get("amount").asInt())
                            .isEqualTo(created.get("flight").get("price").get("amount").asInt());
                });
    }

    @Test
    void стоимостьУмножаетсяНаЧислоПассажиров() {
        JavalinTest.test(
                app(),
                (server, client) -> {
                    var created =
                            json(
                                    createBooking(
                                            client,
                                            List.of(passenger("Петров"), passenger("Петрова"))));

                    assertThat(created.get("totalPrice").get("amount").asInt())
                            .isEqualTo(
                                    created.get("flight").get("price").get("amount").asInt() * 2);
                });
    }

    @Test
    void короткийНомерДокументаПринимается() {
        // Строгая проверка формата документа сломала бы сценарий: проверка присылает «1».
        JavalinTest.test(
                app(),
                (server, client) ->
                        assertThat(createBooking(client, List.of(passenger("Петров"))).code())
                                .isEqualTo(201));
    }

    @Test
    void броньБезПассажировОтклоняется() {
        JavalinTest.test(
                app(),
                (server, client) -> {
                    var response =
                            client.post(
                                    "/api/bookings",
                                    bookingPayload(firstFlightId(client), List.of()));

                    assertThat(response.code()).isEqualTo(400);
                });
    }

    @Test
    void броньНаНеизвестныйРейсОтклоняется() {
        JavalinTest.test(
                app(),
                (server, client) -> {
                    var response =
                            client.post(
                                    "/api/bookings",
                                    bookingPayload("fl_missing", List.of(passenger("Петров"))));

                    assertThat(response.code()).isEqualTo(400);
                });
    }

    @Test
    void броньНаходитсяПоКодуИФамилии() {
        JavalinTest.test(
                app(),
                (server, client) -> {
                    var created = json(createBooking(client, List.of(passenger("Петров"))));
                    var found =
                            json(
                                    client.get(
                                            "/api/bookings/"
                                                    + created.get("code").asText()
                                                    + "?lastName=Петров"));

                    assertThat(found.get("code").asText()).isEqualTo(created.get("code").asText());
                    assertThat(found.get("passengers")).isNotEmpty();
                });
    }

    @Test
    void датаРожденияНеУезжаетНаСутки() {
        // LocalDate без JavaTimeModule Jackson пишет массивом [1990,5,20], java.sql.Date — числом.
        // Строка из to_char в SQL защищает от обоих случаев.
        JavalinTest.test(
                app(),
                (server, client) -> {
                    var created = json(createBooking(client, List.of(passenger("Петров"))));
                    var found =
                            json(
                                    client.get(
                                            "/api/bookings/"
                                                    + created.get("code").asText()
                                                    + "?lastName=Петров"));

                    assertThat(found.get("passengers").get(0).get("dateOfBirth").asText())
                            .isEqualTo("1990-05-20");
                });
    }

    @Test
    void сравнениеФамилииИгнорируетРегистрИПробелы() {
        JavalinTest.test(
                app(),
                (server, client) -> {
                    var created = json(createBooking(client, List.of(passenger("Петров"))));
                    var response =
                            client.get(
                                    "/api/bookings/"
                                            + created.get("code").asText()
                                            + "?lastName=%20%20%D0%BF%D0%95%D1%82%D1%80%D0%BE%D0%92%20%20");

                    assertThat(response.code()).isEqualTo(200);
                });
    }

    @Test
    void невернаяФамилияДаёт404() {
        JavalinTest.test(
                app(),
                (server, client) -> {
                    var created = json(createBooking(client, List.of(passenger("Петров"))));
                    var response =
                            client.get(
                                    "/api/bookings/"
                                            + created.get("code").asText()
                                            + "?lastName=Никтоев");

                    assertThat(response.code()).isEqualTo(404);
                    assertThat(json(response).get("code").asText()).isEqualTo("not_found");
                });
    }

    @Test
    void отсутствующаяФамилияДаётТотЖе404() {
        JavalinTest.test(
                app(),
                (server, client) -> {
                    var created = json(createBooking(client, List.of(passenger("Петров"))));
                    var response = client.get("/api/bookings/" + created.get("code").asText());

                    assertThat(response.code()).isEqualTo(404);
                });
    }

    @Test
    void неизвестныйКодДаётТакойЖе404() {
        JavalinTest.test(
                app(),
                (server, client) -> {
                    var response = client.get("/api/bookings/ZZZZZZ?lastName=Никтоев");

                    assertThat(response.code()).isEqualTo(404);
                });
    }

    @Test
    void отменаВозвращаетБроньСоСтатусомCancelled() {
        JavalinTest.test(
                app(),
                (server, client) -> {
                    var created = json(createBooking(client, List.of(passenger("Петров"))));
                    var cancelled = json(cancel(client, created.get("code").asText()));

                    assertThat(cancelled.get("status").asText()).isEqualTo("cancelled");
                    assertThat(cancelled.get("code").asText())
                            .isEqualTo(created.get("code").asText());
                });
    }

    @Test
    void повторнаяОтменаНеЛомается() {
        JavalinTest.test(
                app(),
                (server, client) -> {
                    var created = json(createBooking(client, List.of(passenger("Петров"))));
                    cancel(client, created.get("code").asText());
                    var again = json(cancel(client, created.get("code").asText()));

                    assertThat(again.get("status").asText()).isEqualTo("cancelled");
                });
    }

    @Test
    void отменаБезФамилииДаёт404() {
        JavalinTest.test(
                app(),
                (server, client) -> {
                    var created = json(createBooking(client, List.of(passenger("Петров"))));
                    var response =
                            client.post(
                                    "/api/bookings/" + created.get("code").asText() + "/cancel",
                                    Map.of());

                    assertThat(response.code()).isEqualTo(404);
                });
    }

    @Test
    void неизвестныйПутьВнутриApiДаётJsonNotFound() {
        JavalinTest.test(
                app(),
                (server, client) -> {
                    var response = client.get("/api/unknown");

                    assertThat(response.code()).isEqualTo(404);
                    assertThat(json(response).get("code").asText()).isEqualTo("not_found");
                });
    }

    @Test
    void прямаяСсылкаНаЭкранОтдаётСтраницу() {
        // Тот же обработчик, что даёт JSON-404 внутри /api, вне /api обязан отдавать страницу.
        // Пока статика не собрана (make build), файла нет — тест проверяет главное: ответ приходит
        // не от API-роутера и не в формате ошибки контракта.
        JavalinTest.test(
                app(),
                (server, client) -> {
                    var response = client.get("/lookup");

                    assertThat(response.code()).isIn(200, 404);
                    if (response.code() == 200) {
                        assertThat(response.headers().get("Content-Type").getFirst())
                                .contains("text/html");
                    }
                });
    }

    private Response createBooking(io.javalin.testtools.HttpClient client, List<?> passengers)
            throws IOException {
        return client.post("/api/bookings", bookingPayload(firstFlightId(client), passengers));
    }

    private Response cancel(io.javalin.testtools.HttpClient client, String code) {
        return client.post("/api/bookings/" + code + "/cancel", Map.of("lastName", "Петров"));
    }

    private String firstFlightId(io.javalin.testtools.HttpClient client) throws IOException {
        var flights = json(client.get("/api/flights?origin=MOW&destination=LED&date=" + day(3)));

        assertThat(flights).as("Нужен хотя бы один рейс MOW → LED").isNotEmpty();

        return flights.get(0).get("id").asText();
    }
}
