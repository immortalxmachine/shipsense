package com.shipsense.service;

import com.shipsense.dto.PredictionResult;
import com.shipsense.dto.ShipmentRequest;
import com.shipsense.dto.ShipmentResponse;
import com.shipsense.entity.Hub;
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

    private static Hub hub(int id, String code, String city, String region) {
        Hub h = new Hub(code, code + " Hub", city, region,
                new BigDecimal("0.0"), new BigDecimal("0.0"));
        ReflectionTestUtils.setField(h, "id", id);
        return h;
    }

    @Test
    void create_persistsShipment_andAttachesPrediction() {
        Hub sp = hub(1, "SP", "São Paulo", "SE");
        Hub rs = hub(2, "RS", "Rio Grande do Sul", "S");
        when(hubService.getByCodeOrThrow("SP")).thenReturn(sp);
        when(hubService.getByCodeOrThrow("RS")).thenReturn(rs);
        when(shipmentRepository.existsByTrackingNo(anyString())).thenReturn(false);
        when(shipmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(routeService.shortestDistanceKm("SP", "RS")).thenReturn(850.0);
        when(mlPredictionClient.predict(any())).thenReturn(new PredictionResult(11.0, 0.8, true));

        ShipmentRequest req = new ShipmentRequest(
                null, "SP", "RS", new BigDecimal("2.5"),
                new BigDecimal("25.90"), OffsetDateTime.now().plusDays(15));

        ShipmentResponse resp = service.create(req);

        assertThat(resp.originCode()).isEqualTo("SP");
        assertThat(resp.destCode()).isEqualTo("RS");
        assertThat(resp.weightKg()).isEqualByComparingTo("2.5");
        assertThat(resp.freightValue()).isEqualByComparingTo("25.90");
        assertThat(resp.trackingNo()).matches("SS\\d{10}");
        assertThat(resp.predictedDeliveryDays()).isEqualTo(11.0);
        assertThat(resp.onTimeProbability()).isEqualTo(0.8);
        verify(shipmentRepository).save(any());
    }

    @Test
    void create_rejectsSameOriginAndDestination() {
        Hub sp = hub(1, "SP", "São Paulo", "SE");
        when(hubService.getByCodeOrThrow("SP")).thenReturn(sp);

        ShipmentRequest req = new ShipmentRequest(
                null, "SP", "SP", new BigDecimal("1.0"),
                new BigDecimal("10.00"), OffsetDateTime.now().plusDays(1));

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
