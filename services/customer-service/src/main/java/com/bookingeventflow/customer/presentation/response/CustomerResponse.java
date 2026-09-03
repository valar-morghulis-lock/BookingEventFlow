package com.bookingeventflow.customer.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "A registered customer.")
public record CustomerResponse(
        UUID id,
        String email,
        String firstName,
        String lastName
) {
}