package com.bookingeventflow.customer.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to register a new customer.")
public record RegisterCustomerRequest(

        @Schema(description = "Customer email, used as login identifier.", example = "jane@example.com")
        @NotBlank(message = "Email must not be blank")
        @Email(message = "Email must be a valid email address")
        String email,

        @Schema(description = "Password, minimum 8 characters.", example = "S3curePass!")
        @NotBlank(message = "Password must not be blank")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password,

        @Schema(description = "First name.", example = "Jane")
        @NotBlank(message = "First name must not be blank")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @Schema(description = "Last name.", example = "Doe")
        @NotBlank(message = "Last name must not be blank")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName
) {
}