package com.bookingeventflow.customer.service;

import com.bookingeventflow.customer.presentation.request.LoginRequest;
import com.bookingeventflow.customer.presentation.request.RegisterCustomerRequest;
import com.bookingeventflow.customer.presentation.response.CustomerResponse;
import com.bookingeventflow.customer.presentation.response.TokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers
@SpringBootTest
class AuthServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("jwt.public-key-path", () -> "classpath:keys/test-public.pem");
        registry.add("jwt.private-key-path", () -> "classpath:keys/test-private.pem");
    }

    @Autowired
    private AuthService authService;

    @Test
    void shouldRegisterThenLoginAndIssueValidToken() {

        RegisterCustomerRequest registerRequest =
                new RegisterCustomerRequest("ahmed@instance.com", "password123", "Ahmed", "Samir");

        CustomerResponse registered = authService.register(registerRequest);

        assertEquals("ahmed@instance.com", registered.email());

        TokenResponse token = authService.login(
                new LoginRequest("ahmed@instance.com", "password123")
        );

        assertNotNull(token.accessToken());

        CustomerResponse fetched = authService.getCurrentCustomer(registered.id());

        assertEquals(registered.email(), fetched.email());
    }

    @Test
    void shouldRejectDuplicateRegistration() {

        authService.register(
                new RegisterCustomerRequest("duplicate@instance.com", "password123", "Ahmed", "Samir")
        );

        assertThrows(
                com.bookingeventflow.customer.exception.EmailAlreadyRegisteredException.class,
                () -> authService.register(
                        new RegisterCustomerRequest("duplicate@instance.com", "password123", "Ahmed", "Samir")
                )
        );
    }
}