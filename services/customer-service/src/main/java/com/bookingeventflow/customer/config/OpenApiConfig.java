package com.bookingeventflow.customer.config;

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
                        .title("BookingEventFlow - Customer Service API")
                        .description("REST API for customer registration and authentication.")
                        .version("v1"));
    }
}