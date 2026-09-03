package com.bookingeventflow.customer.service;

import com.bookingeventflow.customer.entity.CustomerEntity;
import com.bookingeventflow.customer.exception.EmailAlreadyRegisteredException;
import com.bookingeventflow.customer.exception.InvalidCredentialsException;
import com.bookingeventflow.customer.observability.metrics.CustomerMetrics;
import com.bookingeventflow.customer.presentation.request.LoginRequest;
import com.bookingeventflow.customer.presentation.request.RegisterCustomerRequest;
import com.bookingeventflow.customer.presentation.response.CustomerResponse;
import com.bookingeventflow.customer.presentation.response.TokenResponse;
import com.bookingeventflow.customer.repository.CustomerRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    private static final String EMAIL = "ahmed@instance.com";
    private static final String RAW_PASSWORD = "password123";
    private static final String HASHED_PASSWORD = "$2a$hashed";

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtEncoder jwtEncoder;

    private SimpleMeterRegistry meterRegistry;
    private CustomerMetrics customerMetrics;
    private AuthService authService;

    @BeforeEach
    void setUp() {

        meterRegistry = new SimpleMeterRegistry();
        customerMetrics = new CustomerMetrics(meterRegistry);

        authService = new AuthService(
                customerRepository,
                passwordEncoder,
                jwtEncoder,
                customerMetrics
        );
    }

    @Nested
    @DisplayName("register")
    class RegisterTests {

        @Test
        @DisplayName("should register a new customer")
        void shouldRegisterNewCustomer() {

            RegisterCustomerRequest request =
                    new RegisterCustomerRequest(EMAIL, RAW_PASSWORD, "Ahmed", "Samir");

            when(customerRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(HASHED_PASSWORD);
            when(customerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            CustomerResponse response = authService.register(request);

            assertEquals(EMAIL, response.email());
            assertEquals("Ahmed", response.firstName());
            assertEquals("Samir", response.lastName());

            assertMetric("customer.operations", "register", "success", 1.0);
        }

        @Test
        @DisplayName("should throw when email already registered")
        void shouldThrowWhenEmailTaken() {

            RegisterCustomerRequest request =
                    new RegisterCustomerRequest(EMAIL, RAW_PASSWORD, "Ahmed", "Samir");

            when(customerRepository.existsByEmail(EMAIL)).thenReturn(true);

            assertThrows(
                    EmailAlreadyRegisteredException.class,
                    () -> authService.register(request)
            );

            verify(customerRepository, never()).save(any());

            assertMetric("customer.operations", "register", "email_taken", 1.0);
        }
    }

    @Nested
    @DisplayName("login")
    class LoginTests {

        @Test
        @DisplayName("should issue a token for valid credentials")
        void shouldIssueTokenForValidCredentials() {

            LoginRequest request = new LoginRequest(EMAIL, RAW_PASSWORD);

            CustomerEntity customer = new CustomerEntity(EMAIL, HASHED_PASSWORD, "Ahmed", "Samir");

            when(customerRepository.findByEmail(EMAIL)).thenReturn(Optional.of(customer));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);

            Jwt jwt = mock(Jwt.class);
            when(jwt.getTokenValue()).thenReturn("signed-jwt-token");
            when(jwtEncoder.encode(any())).thenReturn(jwt);

            TokenResponse response = authService.login(request);

            assertEquals("signed-jwt-token", response.accessToken());
            assertEquals(3600, response.expiresInSeconds());

            assertMetric("customer.operations", "login", "success", 1.0);
        }

        @Test
        @DisplayName("should throw when email not found")
        void shouldThrowWhenEmailNotFound() {

            LoginRequest request = new LoginRequest(EMAIL, RAW_PASSWORD);

            when(customerRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThrows(
                    InvalidCredentialsException.class,
                    () -> authService.login(request)
            );

            assertMetric("customer.operations", "login", "invalid_credentials", 1.0);
        }

        @Test
        @DisplayName("should throw when password does not match")
        void shouldThrowWhenPasswordDoesNotMatch() {

            LoginRequest request = new LoginRequest(EMAIL, "wrong-password");

            CustomerEntity customer = new CustomerEntity(EMAIL, HASHED_PASSWORD, "Ahmed", "Samir");

            when(customerRepository.findByEmail(EMAIL)).thenReturn(Optional.of(customer));
            when(passwordEncoder.matches("wrong-password", HASHED_PASSWORD)).thenReturn(false);

            assertThrows(
                    InvalidCredentialsException.class,
                    () -> authService.login(request)
            );

            assertMetric("customer.operations", "login", "invalid_credentials", 1.0);
        }
    }

    @Nested
    @DisplayName("getCurrentCustomer")
    class GetCurrentCustomerTests {

        @Test
        @DisplayName("should return the customer when found")
        void shouldReturnCustomerWhenFound() {

            CustomerEntity customer = new CustomerEntity(EMAIL, HASHED_PASSWORD, "Ahmed", "Samir");
            UUID customerId = customer.id();

            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

            CustomerResponse response = authService.getCurrentCustomer(customerId);

            assertEquals(EMAIL, response.email());

            assertMetric("customer.operations", "get_current_customer", "success", 1.0);
        }

        @Test
        @DisplayName("should throw when customer no longer exists")
        void shouldThrowWhenCustomerNotFound() {

            UUID customerId = UUID.randomUUID();

            when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

            assertThrows(
                    InvalidCredentialsException.class,
                    () -> authService.getCurrentCustomer(customerId)
            );

            assertMetric("customer.operations", "get_current_customer", "not_found", 1.0);
        }
    }

    private void assertMetric(String metricName, String operation, String result, double expectedCount) {
        assertEquals(
                expectedCount,
                meterRegistry.get(metricName)
                        .tags("operation", operation, "result", result)
                        .counter()
                        .count()
        );
    }
}