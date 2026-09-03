package com.bookingeventflow.customer.controller;

import com.bookingeventflow.customer.exception.RateLimitExceededException;
import com.bookingeventflow.customer.presentation.request.LoginRequest;
import com.bookingeventflow.customer.presentation.request.RegisterCustomerRequest;
import com.bookingeventflow.customer.presentation.response.CustomerResponse;
import com.bookingeventflow.customer.presentation.response.TokenResponse;
import com.bookingeventflow.customer.service.AuthService;
import com.bookingeventflow.customer.service.RateLimiterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Customer registration and authentication.")
public class AuthController {

    private final AuthService authService;
    private final RateLimiterService rateLimiterService;

    public AuthController(AuthService authService, RateLimiterService rateLimiterService) {
        this.authService = authService;
        this.rateLimiterService = rateLimiterService;
    }

    @Operation(summary = "Register a new customer")
    @PostMapping("/register")
    public ResponseEntity<CustomerResponse> register(
            @Valid @RequestBody RegisterCustomerRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = resolveClientIp(httpRequest);

        if (!rateLimiterService.tryConsumeRegister(clientIp)) {
            throw new RateLimitExceededException();
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Operation(summary = "Authenticate and receive an access token")
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = resolveClientIp(httpRequest);
        String normalizedEmail = request.email().toLowerCase();
        String ipAndEmailKey = clientIp + ":" + normalizedEmail;

        // Order matters only for short-circuiting cost, not correctness —
        // check the broadest, cheapest-to-trigger limit first.
        if (!rateLimiterService.tryConsumeLoginByIp(clientIp)) {
            throw new RateLimitExceededException();
        }

        if (!rateLimiterService.tryConsumeLoginByEmail(normalizedEmail)) {
            throw new RateLimitExceededException();
        }

        if (!rateLimiterService.tryConsumeLogin(ipAndEmailKey)) {
            throw new RateLimitExceededException();
        }

        return ResponseEntity.ok(authService.login(request));
    }

    private String resolveClientIp(HttpServletRequest request) {

        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}