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
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final long TOKEN_TTL_SECONDS = 3600;

    private static final String OP_REGISTER = "register";
    private static final String OP_LOGIN = "login";
    private static final String OP_GET_CURRENT = "get_current_customer";

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final CustomerMetrics customerMetrics;

    public AuthService(
            CustomerRepository customerRepository,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            CustomerMetrics customerMetrics
    ) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.customerMetrics = customerMetrics;
    }

    @Timed(value = "customer.register.duration", description = "Customer registration duration")
    @Transactional
    public CustomerResponse register(RegisterCustomerRequest request) {

        if (customerRepository.existsByEmail(request.email())) {

            customerMetrics.recordOperation(OP_REGISTER, CustomerMetrics.Result.EMAIL_TAKEN);

            throw new EmailAlreadyRegisteredException(request.email());
        }

        CustomerEntity customer = customerRepository.save(
                new CustomerEntity(
                        request.email(),
                        passwordEncoder.encode(request.password()),
                        request.firstName(),
                        request.lastName()
                )
        );

        customerMetrics.recordOperation(OP_REGISTER, CustomerMetrics.Result.SUCCESS);

        log.info("Registered customer {}", customer.id());

        return toResponse(customer);
    }

    @Timed(value = "customer.login.duration", description = "Customer login duration")
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {

        CustomerEntity customer = customerRepository.findByEmail(request.email())
                .orElseGet(() -> {
                    customerMetrics.recordOperation(OP_LOGIN, CustomerMetrics.Result.INVALID_CREDENTIALS);
                    throw new InvalidCredentialsException();
                });

        if (!passwordEncoder.matches(request.password(), customer.getPasswordHash())) {

            customerMetrics.recordOperation(OP_LOGIN, CustomerMetrics.Result.INVALID_CREDENTIALS);

            throw new InvalidCredentialsException();
        }

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("customer-service")
                .issuedAt(now)
                .expiresAt(now.plus(TOKEN_TTL_SECONDS, ChronoUnit.SECONDS))
                .subject(customer.id().toString())
                .claim("email", customer.getEmail())
                .claim("role", customer.getRole().name())
                .build();

        String token = jwtEncoder.encode(
                JwtEncoderParameters.from(
                        JwsHeader.with(SignatureAlgorithm.RS256).build(),
                        claims
                )
        ).getTokenValue();

        customerMetrics.recordOperation(OP_LOGIN, CustomerMetrics.Result.SUCCESS);

        log.info("Customer {} logged in", customer.id());

        return new TokenResponse(token, TOKEN_TTL_SECONDS);
    }

    @Timed(value = "customer.get_current.duration", description = "Fetch current authenticated customer duration")
    @Transactional(readOnly = true)
    public CustomerResponse getCurrentCustomer(UUID customerId) {

        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> {
                    customerMetrics.recordOperation(OP_GET_CURRENT, CustomerMetrics.Result.NOT_FOUND);
                    return new InvalidCredentialsException();
                });

        customerMetrics.recordOperation(OP_GET_CURRENT, CustomerMetrics.Result.SUCCESS);

        return toResponse(customer);
    }

    private CustomerResponse toResponse(CustomerEntity customer) {
        return new CustomerResponse(
                customer.id(),
                customer.getEmail(),
                customer.getFirstName(),
                customer.getLastName()
        );
    }
}