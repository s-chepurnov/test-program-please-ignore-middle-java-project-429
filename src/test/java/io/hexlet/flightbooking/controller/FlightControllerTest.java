package io.hexlet.flightbooking.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class FlightControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static String date(int plusDays) {
        return LocalDate.now(ZoneOffset.UTC).plusDays(plusDays).toString();
    }

    // ---------- поиск ----------

    @Test
    void searchReturnsFlights() throws Exception {
        mockMvc.perform(get("/api/flights")
                        .param("origin", "MOW")
                        .param("destination", "LED")
                        .param("date", date(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$[0].id").isString())
                .andExpect(jsonPath("$[0].flightNumber").isString())
                .andExpect(jsonPath("$[0].airline.code").isString())
                .andExpect(jsonPath("$[0].airline.name").isString())
                .andExpect(jsonPath("$[0].origin.code").value("MOW"))
                .andExpect(jsonPath("$[0].origin.name").isString())
                .andExpect(jsonPath("$[0].destination.code").value("LED"))
                .andExpect(jsonPath("$[0].departureAt")
                        .value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z")))
                .andExpect(jsonPath("$[0].arrivalAt")
                        .value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z")))
                .andExpect(jsonPath("$[0].durationMinutes").isNumber())
                .andExpect(jsonPath("$[0].price.amount").isNumber())
                .andExpect(jsonPath("$[0].price.currency").value("RUB"))
                .andExpect(jsonPath("$[0].seatsAvailable").isNumber());
    }

    @Test
    void searchReturnsEmptyArrayWhenNothingFound() throws Exception {
        // Дата далеко за пределами окна Seeder (30 дней от сегодня)
        mockMvc.perform(get("/api/flights")
                        .param("origin", "MOW")
                        .param("destination", "LED")
                        .param("date", date(365)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void searchReturnsEmptyArrayForUnknownRoute() throws Exception {
        mockMvc.perform(get("/api/flights")
                        .param("origin", "XXX")
                        .param("destination", "YYY")
                        .param("date", date(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ---------- отсутствующий параметр date ----------

    @Test
    void searchWithoutDateReturns400() throws Exception {
        mockMvc.perform(get("/api/flights")
                        .param("origin", "MOW")
                        .param("destination", "LED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("validation_error"))
                .andExpect(jsonPath("$.message").isString());
    }

    // ---------- получение по id ----------

    @Test
    void getByIdReturnsFlight() throws Exception {
        // Сначала найдём реальный id через поиск
        var body = mockMvc.perform(get("/api/flights")
                        .param("origin", "MOW")
                        .param("destination", "LED")
                        .param("date", date(1)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var id = JsonPath.<String>read(body, "$[0].id");

        mockMvc.perform(get("/api/flights/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.origin.code").value("MOW"))
                .andExpect(jsonPath("$.destination.code").value("LED"))
                .andExpect(jsonPath("$.departureAt")
                        .value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z")))
                .andExpect(jsonPath("$.price.currency").value("RUB"));
    }

    @Test
    void getByIdReturnsSameShapeAsSearch() throws Exception {
        // id, найденный через search, должен отдаваться в том же виде
        var searchBody = mockMvc.perform(get("/api/flights")
                        .param("origin", "MOW")
                        .param("destination", "LED")
                        .param("date", date(1)))
                .andReturn().getResponse().getContentAsString();

        var id = JsonPath.<String>read(searchBody, "$[0].id");

        var byIdBody = mockMvc.perform(get("/api/flights/{id}", id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Сравниваем наборы ключей верхнего уровня
        var searchKeys = JsonPath.<java.util.Map<String, Object>>read(searchBody, "$[0]").keySet();
        var byIdKeys = JsonPath.<java.util.Map<String, Object>>read(byIdBody, "$").keySet();

        assertThat(byIdKeys)
                .containsExactlyInAnyOrderElementsOf(searchKeys);
    }

    @Test
    void getByIdReturns404ForUnknownId() throws Exception {
        mockMvc.perform(get("/api/flights/{id}", "fl_does_not_exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("not_found"))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void searchUsesUtcForDateComparison() throws Exception {
        // берём дату, на которую Seeder создал рейсы
        var tomorrow = LocalDate.now(ZoneOffset.UTC).plusDays(1).toString();

        var body = mockMvc.perform(get("/api/flights")
                        .param("origin", "MOW")
                        .param("destination", "LED")
                        .param("date", tomorrow))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // все departureAt должны быть в UTC-дне, который запросили
        var count = JsonPath.<List<Object>>read(body, "$");
        assertThat(count).isNotEmpty();

        for (var item : count) {
            var departureAt = (String) ((Map<?, ?>) item).get("departureAt");
            assertThat(departureAt).startsWith(tomorrow);   // "2026-09-14T..." — UTC-дата совпадает с запрошенной
        }
    }
}
