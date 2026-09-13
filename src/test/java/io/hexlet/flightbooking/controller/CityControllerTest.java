package io.hexlet.flightbooking.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(
        scripts = "/cleandatabase.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listReturnsSeededCities() throws Exception {
        mockMvc.perform(get("/api/cities"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[0].code").value("MOW"))
                .andExpect(jsonPath("$[0].name").value("Москва"))
                .andExpect(jsonPath("$[0].country").value("Россия"))

                .andExpect(jsonPath("$[1].code").value("LED"))
                .andExpect(jsonPath("$[2].code").value("AER"))
                .andExpect(jsonPath("$[3].code").value("KZN"))
                .andExpect(jsonPath("$[4].code").value("SVX"))
                .andExpect(jsonPath("$[5].code").value("OVB"))
                .andExpect(jsonPath("$[6].code").value("KGD"));
    }

    @Test
    void listReturnsCitiesInSortOrder() throws Exception {

        mockMvc.perform(get("/api/cities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("MOW"))
                .andExpect(jsonPath("$[1].code").value("LED"));
    }
}
