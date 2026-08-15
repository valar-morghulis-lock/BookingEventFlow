package com.bookingeventflow.event.config;

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
                        .title("BookingEventFlow - Event Service API")
                        .description(
                                "REST API for event management and discovery."
                        )
                        .version("v1"));
    }
}