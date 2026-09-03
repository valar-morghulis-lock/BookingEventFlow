package com.bookingeventflow.customer.controller;

import com.bookingeventflow.customer.presentation.response.CustomerResponse;
import com.bookingeventflow.customer.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customers", description = "Authenticated customer profile operations.")
public class CustomerController {

    private final AuthService authService;

    public CustomerController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Get the currently authenticated customer's profile")
    @GetMapping("/me")
    public ResponseEntity<CustomerResponse> getCurrentCustomer(JwtAuthenticationToken authentication) {

        Jwt jwt = authentication.getToken();
        UUID customerId = UUID.fromString(jwt.getSubject());

        return ResponseEntity.ok(authService.getCurrentCustomer(customerId));
    }
}