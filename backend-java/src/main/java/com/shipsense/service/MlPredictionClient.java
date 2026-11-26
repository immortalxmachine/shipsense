package com.shipsense.service;

import com.shipsense.dto.MlPredictRequest;
import com.shipsense.dto.MlPredictResponse;
import com.shipsense.dto.PredictionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Thin client over the Python ML service.
 *
 * <p><b>Resilience:</b> a delay prediction is a "nice to have" enrichment, not
 * core to tracking a parcel. If the ML service is down or errors, we log it and
 * return {@link PredictionResult#unavailable()} so the API keeps working
 * (graceful degradation) instead of failing the whole request.
 */
@Service
public class MlPredictionClient {

    private static final Logger log = LoggerFactory.getLogger(MlPredictionClient.class);

    private final RestClient mlRestClient;

    public MlPredictionClient(RestClient mlRestClient) {
        this.mlRestClient = mlRestClient;
    }

    public PredictionResult predict(MlPredictRequest request) {
        try {
            MlPredictResponse body = mlRestClient.post()
                    .uri("/predict")
                    .body(request)
                    .retrieve()
                    .body(MlPredictResponse.class);

            if (body == null) {
                return PredictionResult.unavailable();
            }
            return new PredictionResult(body.predictedDeliveryDays(),
                    body.onTimeProbability(), true);

        } catch (Exception ex) {
            // Any failure (connection refused, timeout, 5xx, bad JSON) -> fallback.
            log.warn("ML service unavailable, returning fallback prediction: {}", ex.getMessage());
            return PredictionResult.unavailable();
        }
    }
}
