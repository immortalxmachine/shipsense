package com.shipsense.service;

import com.shipsense.dto.PredictionResult;
import com.shipsense.dto.ShipmentRequest;
import com.shipsense.dto.ShipmentResponse;
import com.shipsense.entity.Hub;
import com.shipsense.entity.ServiceType;
import com.shipsense.exception.BadRequestException;
import com.shipsense.exception.NotFoundException;
import com.shipsense.repository.ShipmentRepository;
import com.shipsense.repository.TrackingEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Unit tests for {@link ShipmentService} with mocked collaborators. */
@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {

    @Mock ShipmentRepository shipmentRepository;
    @Mock TrackingEventRepository eventRepository;
    @Mock HubService hubService;
    @Mock RouteService routeService;
    @Mock MlPredictionClient mlPredictionClient;

    @InjectMocks ShipmentService service;

    private static Hub hub(int id, String code, String city) {
        Hub h = new Hub(code, code + " Hub", city, new BigDecimal("0.0"), new BigDecimal("0.0"));
        ReflectionTestUtils.setField(h, "id", id);
        return h;
    }

    @Test
    void create_persistsShipment_andAttachesPrediction() {
        Hub del = hub(1, "DEL", "Delhi");
        Hub chn = hub(2, "CHN", "Chennai");
        when(hubService.getByCodeOrThrow("DEL")).thenReturn(del);
        when(hubService.getByCodeOrThrow("CHN")).thenReturn(chn);
        when(shipmentRepository.existsByTrackingNo(anyString())).thenReturn(false);
        when(shipmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(routeService.shortestDistanceKm("DEL", "CHN")).thenReturn(2000.0);
        when(shipmentRepository.countByOriginHubId(1)).thenReturn(5L);
        when(mlPredictionClient.predict(any())).thenReturn(new PredictionResult(10.0, 0.8, true));

        ShipmentRequest req = new ShipmentRequest(
                null, "DEL", "CHN", new BigDecimal("2.5"),
                ServiceType.EXPRESS, OffsetDateTime.now().plusDays(2));

        ShipmentResponse resp = service.create(req);

        assertThat(resp.originCode()).isEqualTo("DEL");
        assertThat(resp.destCode()).isEqualTo("CHN");
        assertThat(resp.weightKg()).isEqualByComparingTo("2.5");
        assertThat(resp.trackingNo()).matches("SS\\d{10}");
        assertThat(resp.predictedDelayHours()).isEqualTo(10.0);
        assertThat(resp.onTimeProbability()).isEqualTo(0.8);
        verify(shipmentRepository).save(any());
    }

    @Test
    void create_rejectsSameOriginAndDestination() {
        Hub del = hub(1, "DEL", "Delhi");
        when(hubService.getByCodeOrThrow("DEL")).thenReturn(del);

        ShipmentRequest req = new ShipmentRequest(
                null, "DEL", "DEL", new BigDecimal("1.0"),
                ServiceType.STANDARD, OffsetDateTime.now().plusDays(1));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("differ");
        verify(shipmentRepository, never()).save(any());
    }

    @Test
    void getByTrackingNo_throwsWhenMissing() {
        when(shipmentRepository.findByTrackingNo("SS9999999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByTrackingNo("SS9999999999"))
                .isInstanceOf(NotFoundException.class);
    }
}
