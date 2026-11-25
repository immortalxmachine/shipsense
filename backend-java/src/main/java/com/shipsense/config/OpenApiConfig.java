package com.shipsense.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger metadata. springdoc serves the interactive UI at
 * {@code /swagger-ui.html} and the raw spec at {@code /v3/api-docs}.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI shipSenseOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("ShipSense API")
                .description("Parcel delivery ETA prediction & route optimization")
                .version("0.1.0")
                .license(new License().name("MIT")));
    }
}
