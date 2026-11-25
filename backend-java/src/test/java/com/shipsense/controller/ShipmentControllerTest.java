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

        Hub del = hubRepository.save(new Hub("DEL", "Delhi Hub", "Delhi",
                new BigDecimal("28.6"), new BigDecimal("77.2")));
        Hub chn = hubRepository.save(new Hub("CHN", "Chennai Hub", "Chennai",
                new BigDecimal("13.0"), new BigDecimal("80.2")));
        Hub blr = hubRepository.save(new Hub("BLR", "Bengaluru Hub", "Bengaluru",
                new BigDecimal("12.9"), new BigDecimal("77.5")));

        // CHN-BLR-DEL (350+2000) is shorter than direct CHN-DEL (2500).
        saveBoth(chn, blr, 350, 6);
        saveBoth(blr, del, 2000, 33);
        saveBoth(chn, del, 2500, 42);

        when(mlPredictionClient.predict(any()))
                .thenReturn(new PredictionResult(12.5, 0.77, true));
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
                .andExpect(jsonPath("$[*].code", hasItems("DEL", "CHN", "BLR")));
    }

    @Test
    void optimalRoute_returnsShortestPath() throws Exception {
        mockMvc.perform(get("/api/routes/optimal").param("from", "CHN").param("to", "DEL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path", contains("CHN", "BLR", "DEL")))
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
                  "originHubCode": "DEL",
                  "destHubCode": "CHN",
                  "weightKg": 2.5,
                  "serviceType": "EXPRESS",
                  "promisedAt": "2030-01-01T00:00:00Z"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.trackingNo", matchesPattern("SS\\d{10}")))
                .andExpect(jsonPath("$.predictedDelayHours", is(12.5)))
                .andReturn();

        String trackingNo = JsonPath.read(result.getResponse().getContentAsString(), "$.trackingNo");

        mockMvc.perform(get("/api/shipments/{trackingNo}", trackingNo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originCode", is("DEL")))
                .andExpect(jsonPath("$.destCode", is("CHN")))
                .andExpect(jsonPath("$.onTimeProbability", is(0.77)));
    }
}
