package com.shipsense.controller;

import com.jayway.jsonpath.JsonPath;
import com.shipsense.dto.PredictionResult;
import com.shipsense.entity.Hub;
import com.shipsense.entity.Route;
import com.shipsense.repository.HubRepository;
import com.shipsense.repository.RouteRepository;
import com.shipsense.repository.ShipmentRepository;
import com.shipsense.repository.TrackingEventRepository;
import com.shipsense.service.MlPredictionClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-stack test of the web layer against an in-memory H2 database.
 * The ML service client is mocked so no network call is made.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ShipmentControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired HubRepository hubRepository;
    @Autowired RouteRepository routeRepository;
    @Autowired ShipmentRepository shipmentRepository;
    @Autowired TrackingEventRepository eventRepository;

    @MockBean MlPredictionClient mlPredictionClient;

    @BeforeEach
    void seed() {
        eventRepository.deleteAll();
        shipmentRepository.deleteAll();
        routeRepository.deleteAll();
        hubRepository.deleteAll();

        Hub rs = hubRepository.save(new Hub("RS", "Rio Grande do Sul Hub", "Rio Grande do Sul",
                "S", new BigDecimal("-29.7"), new BigDecimal("-52.0")));
        Hub sp = hubRepository.save(new Hub("SP", "São Paulo Hub", "São Paulo",
                "SE", new BigDecimal("-23.2"), new BigDecimal("-47.1")));
        Hub mg = hubRepository.save(new Hub("MG", "Minas Gerais Hub", "Minas Gerais",
                "SE", new BigDecimal("-19.9"), new BigDecimal("-44.4")));

        // SP-MG-RS (350+2000) is shorter than direct SP-RS (2500).
        saveBoth(sp, mg, 350, 6);
        saveBoth(mg, rs, 2000, 33);
        saveBoth(sp, rs, 2500, 42);

        when(mlPredictionClient.predict(any()))
                .thenReturn(new PredictionResult(11.5, 0.77, true));
    }

    private void saveBoth(Hub a, Hub b, double km, double hrs) {
        routeRepository.save(new Route(a, b, new BigDecimal(String.valueOf(km)), new BigDecimal(String.valueOf(hrs))));
        routeRepository.save(new Route(b, a, new BigDecimal(String.valueOf(km)), new BigDecimal(String.valueOf(hrs))));
    }

    @Test
    void listHubs_returnsSeededHubs() throws Exception {
        mockMvc.perform(get("/api/hubs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].code", hasItems("RS", "SP", "MG")));
    }

    @Test
    void optimalRoute_returnsShortestPath() throws Exception {
        mockMvc.perform(get("/api/routes/optimal").param("from", "SP").param("to", "RS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path", contains("SP", "MG", "RS")))
                .andExpect(jsonPath("$.totalDistanceKm", is(2350.0)));
    }

    @Test
    void createShipment_withInvalidBody_returns400() throws Exception {
        mockMvc.perform(post("/api/shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    @Test
    void createThenTrackShipment_includesPrediction() throws Exception {
        String body = """
                {
                  "originHubCode": "SP",
                  "destHubCode": "RS",
                  "weightKg": 2.5,
                  "freightValue": 25.90,
                  "promisedAt": "2030-01-01T00:00:00Z"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.trackingNo", matchesPattern("SS\\d{10}")))
                .andExpect(jsonPath("$.predictedDeliveryDays", is(11.5)))
                .andReturn();

        String trackingNo = JsonPath.read(result.getResponse().getContentAsString(), "$.trackingNo");

        mockMvc.perform(get("/api/shipments/{trackingNo}", trackingNo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originCode", is("SP")))
                .andExpect(jsonPath("$.destCode", is("RS")))
                .andExpect(jsonPath("$.onTimeProbability", is(0.77)));
    }
}
