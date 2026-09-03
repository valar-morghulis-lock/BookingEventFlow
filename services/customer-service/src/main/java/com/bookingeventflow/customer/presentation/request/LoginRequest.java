package com.bookingeventflow.customer.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to authenticate and receive an access token.")
public record LoginRequest(

        @Schema(example = "jane@example.com")
        @NotBlank(message = "Email must not be blank")
        @Email(message = "Email must be a valid email address")
        String email,

        @Schema(example = "S3curePass!")
        @NotBlank(message = "Password must not be blank")
        String password
) {
}