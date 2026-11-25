package com.shipsense;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the ShipSense core REST API.
 *
 * <p>{@code @SpringBootApplication} bundles three annotations:
 * {@code @Configuration} (this class can define beans),
 * {@code @EnableAutoConfiguration} (Spring Boot configures the web server,
 * JPA, Jackson, etc. from the classpath) and {@code @ComponentScan}
 * (discovers controllers/services/repositories under this package).
 */
@SpringBootApplication
public class ShipSenseApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShipSenseApplication.class, args);
    }
}
