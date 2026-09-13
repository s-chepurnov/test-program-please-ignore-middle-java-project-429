package io.hexlet.flightbooking.controller;

import io.hexlet.flightbooking.dto.BookingCreateDto;
import io.hexlet.flightbooking.dto.ContactDto;
import io.hexlet.flightbooking.dto.FlightDto;
import io.hexlet.flightbooking.dto.PassengerDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(
        scripts = "/cleandatabase.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class BookingControllerTest {

    private static final String ISO_UTC_PATTERN = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z";

    private static final String NOT_FOUND_MESSAGE = "Бронь не найдена";
    private static final String LAST_NAME = "Петров";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    private String flightId;

    @BeforeEach
    void pickFlight() throws Exception {
        var tomorrow = LocalDate.now(ZoneOffset.UTC).plusDays(1).toString();

        var body = mockMvc.perform(get("/api/flights")
                        .param("origin", "MOW")
                        .param("destination", "LED")
                        .param("date", tomorrow))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        flightId = jsonMapper.readValue(body, FlightDto[].class)[0].id();
    }

    private String createBooking() throws Exception {
        var request = new BookingCreateDto(
                flightId,
                new ContactDto("ivan@example.com", "+79991234567"),
                List.of(new PassengerDto("Сергей", LAST_NAME, "1987-12-01", "1")));

        var body = mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return jsonMapper.readTree(body).get("code").asText();
    }

    private String json(Object value) throws Exception {
        return jsonMapper.writeValueAsString(value);
    }

    @Nested
    class ViewBooking {

        @Test
        void findsBookingByCodeAndLastName() throws Exception {
            var code = createBooking();

            mockMvc.perform(get("/api/bookings/{code}", code)
                            .param("lastName", LAST_NAME))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(code))
                    .andExpect(jsonPath("$.status").value("confirmed"))
                    .andExpect(jsonPath("$.passengers[0].lastName").value(LAST_NAME))
                    .andExpect(jsonPath("$.passengers[0].firstName").value("Сергей"))
                    .andExpect(jsonPath("$.contact.email").value("ivan@example.com"))
                    .andExpect(jsonPath("$.createdAt").value(matchesPattern(ISO_UTC_PATTERN)));
        }

        @Test
        void findsBookingWithDifferentCaseLastName() throws Exception {
            var code = createBooking();

            mockMvc.perform(get("/api/bookings/{code}", code)
                            .param("lastName", "петров"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(code));
        }

        @Test
        void findsBookingWithSurroundingSpaces() throws Exception {
            var code = createBooking();

            mockMvc.perform(get("/api/bookings/{code}", code)
                            .param("lastName", "  " + LAST_NAME + "  "))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(code));
        }

        @Test
        void findsBookingWithLowercaseCode() throws Exception {
            var code = createBooking();

            mockMvc.perform(get("/api/bookings/{code}", code.toLowerCase())
                            .param("lastName", LAST_NAME))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(code));
        }

        @Test
        void wrongLastNameReturns404() throws Exception {
            var code = createBooking();

            mockMvc.perform(get("/api/bookings/{code}", code)
                            .param("lastName", "Сидоров"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("not_found"))
                    .andExpect(jsonPath("$.message").value(NOT_FOUND_MESSAGE));
        }

        @Test
        void missingLastNameReturns404() throws Exception {
            var code = createBooking();

            mockMvc.perform(get("/api/bookings/{code}", code))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("not_found"))
                    .andExpect(jsonPath("$.message").value(NOT_FOUND_MESSAGE));
        }

        @Test
        void blankLastNameReturns404() throws Exception {
            var code = createBooking();

            mockMvc.perform(get("/api/bookings/{code}", code)
                            .param("lastName", "   "))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("not_found"))
                    .andExpect(jsonPath("$.message").value(NOT_FOUND_MESSAGE));
        }

        @Test
        void unknownCodeReturns404() throws Exception {
            mockMvc.perform(get("/api/bookings/{code}", "ZZZZZZ")
                            .param("lastName", LAST_NAME))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("not_found"))
                    .andExpect(jsonPath("$.message").value(NOT_FOUND_MESSAGE));
        }

        @Test
        void allViewFailuresReturnIdenticalBody() throws Exception {
            var code = createBooking();

            var wrongName = mockMvc.perform(get("/api/bookings/{code}", code)
                            .param("lastName", "Сидоров"))
                    .andReturn().getResponse().getContentAsString();

            var noName = mockMvc.perform(get("/api/bookings/{code}", code))
                    .andReturn().getResponse().getContentAsString();

            var blankName = mockMvc.perform(get("/api/bookings/{code}", code)
                            .param("lastName", "   "))
                    .andReturn().getResponse().getContentAsString();

            var wrongCode = mockMvc.perform(get("/api/bookings/{code}", "ZZZZZZ")
                            .param("lastName", LAST_NAME))
                    .andReturn().getResponse().getContentAsString();

            assertThat(wrongName).isEqualTo(noName);
            assertThat(noName).isEqualTo(blankName);
            assertThat(blankName).isEqualTo(wrongCode);
        }
    }

    @Nested
    class CancelBooking {

        @Test
        void cancelReturnsCancelledBooking() throws Exception {
            var code = createBooking();

            mockMvc.perform(post("/api/bookings/{code}/cancel", code)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new LastNameBody(LAST_NAME))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(code))
                    .andExpect(jsonPath("$.status").value("cancelled"))
                    .andExpect(jsonPath("$.passengers[0].lastName").value(LAST_NAME))
                    .andExpect(jsonPath("$.createdAt").value(matchesPattern(ISO_UTC_PATTERN)));
        }

        @Test
        void cancelIsIdempotent() throws Exception {
            var code = createBooking();

            mockMvc.perform(post("/api/bookings/{code}/cancel", code)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new LastNameBody(LAST_NAME))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("cancelled"));

            mockMvc.perform(post("/api/bookings/{code}/cancel", code)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new LastNameBody(LAST_NAME))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("cancelled"));
        }

        @Test
        void cancelWithDifferentCaseLastName() throws Exception {
            var code = createBooking();

            mockMvc.perform(post("/api/bookings/{code}/cancel", code)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new LastNameBody("петров"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("cancelled"));
        }

        @Test
        void cancelWithLowercaseCode() throws Exception {
            var code = createBooking();

            mockMvc.perform(post("/api/bookings/{code}/cancel", code.toLowerCase())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new LastNameBody(LAST_NAME))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("cancelled"));
        }

        @Test
        void cancelPersistsStatusAcrossRequests() throws Exception {
            var code = createBooking();

            // отменяем
            mockMvc.perform(post("/api/bookings/{code}/cancel", code)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new LastNameBody(LAST_NAME))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("cancelled"));

            // просмотр возвращает тот же статус — значит, он сохранён в БД
            mockMvc.perform(get("/api/bookings/{code}", code)
                            .param("lastName", LAST_NAME))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("cancelled"));
        }

        @Test
        void cancelWithWrongLastNameReturns404() throws Exception {
            var code = createBooking();

            mockMvc.perform(post("/api/bookings/{code}/cancel", code)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new LastNameBody("Сидоров"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("not_found"))
                    .andExpect(jsonPath("$.message").value(NOT_FOUND_MESSAGE));
        }

        @Test
        void cancelWithoutLastNameReturns404() throws Exception {
            var code = createBooking();

            mockMvc.perform(post("/api/bookings/{code}/cancel", code)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("not_found"))
                    .andExpect(jsonPath("$.message").value(NOT_FOUND_MESSAGE));
        }

        @Test
        void cancelWithoutBodyReturns404() throws Exception {
            var code = createBooking();

            mockMvc.perform(post("/api/bookings/{code}/cancel", code))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("not_found"))
                    .andExpect(jsonPath("$.message").value(NOT_FOUND_MESSAGE));
        }

        @Test
        void cancelWithBlankLastNameReturns404() throws Exception {
            var code = createBooking();

            mockMvc.perform(post("/api/bookings/{code}/cancel", code)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new LastNameBody("   "))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("not_found"))
                    .andExpect(jsonPath("$.message").value(NOT_FOUND_MESSAGE));
        }

        @Test
        void cancelWithUnknownCodeReturns404() throws Exception {
            mockMvc.perform(post("/api/bookings/{code}/cancel", "ZZZZZZ")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new LastNameBody(LAST_NAME))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("not_found"))
                    .andExpect(jsonPath("$.message").value(NOT_FOUND_MESSAGE));
        }

        @Test
        void allCancelFailuresReturnIdenticalBody() throws Exception {
            var code = createBooking();

            var wrongName = mockMvc.perform(post("/api/bookings/{code}/cancel", code)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new LastNameBody("Сидоров"))))
                    .andReturn().getResponse().getContentAsString();

            var emptyBody = mockMvc.perform(post("/api/bookings/{code}/cancel", code)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andReturn().getResponse().getContentAsString();

            var noBody = mockMvc.perform(post("/api/bookings/{code}/cancel", code))
                    .andReturn().getResponse().getContentAsString();

            var blankName = mockMvc.perform(post("/api/bookings/{code}/cancel", code)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new LastNameBody("   "))))
                    .andReturn().getResponse().getContentAsString();

            var wrongCode = mockMvc.perform(post("/api/bookings/{code}/cancel", "ZZZZZZ")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(new LastNameBody(LAST_NAME))))
                    .andReturn().getResponse().getContentAsString();

            assertThat(wrongName).isEqualTo(emptyBody);
            assertThat(emptyBody).isEqualTo(noBody);
            assertThat(noBody).isEqualTo(blankName);
            assertThat(blankName).isEqualTo(wrongCode);
        }

        private record LastNameBody(String lastName) {}
    }
}
