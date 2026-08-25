package com.bookingeventflow.reservation.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bookingEventFlowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BookingEventFlow - Reservation Service API")
                        .description(
                                "REST API for seat reservations and holds."
                        )
                        .version("v1"));
    }
}