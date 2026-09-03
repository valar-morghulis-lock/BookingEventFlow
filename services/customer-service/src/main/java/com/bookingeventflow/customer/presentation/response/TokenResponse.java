package com.bookingeventflow.customer.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "An issued access token.")
public record TokenResponse(
        String accessToken,
        long expiresInSeconds
) {
}