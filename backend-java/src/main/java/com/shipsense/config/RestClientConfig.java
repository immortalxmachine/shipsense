package com.shipsense.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Configures the {@link RestClient} the backend uses to call the Python ML
 * service. Short timeouts ensure that if the ML service is slow or down, the
 * request fails fast and the caller can fall back gracefully.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient mlRestClient(@Value("${ml.service.url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }
}
