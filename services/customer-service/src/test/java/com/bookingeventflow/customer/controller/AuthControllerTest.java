package com.bookingeventflow.customer.controller;

import com.bookingeventflow.customer.config.SecurityConfig;
import com.bookingeventflow.customer.exception.EmailAlreadyRegisteredException;
import com.bookingeventflow.customer.exception.GlobalExceptionHandler;
import com.bookingeventflow.customer.exception.InvalidCredentialsException;
import com.bookingeventflow.customer.presentation.response.CustomerResponse;
import com.bookingeventflow.customer.presentation.response.TokenResponse;
import com.bookingeventflow.customer.service.AuthService;
import com.bookingeventflow.customer.service.RateLimiterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RateLimiterService rateLimiterService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void register_shouldReturn201_onSuccess() throws Exception {

        when(authService.register(any())).thenReturn(
                new CustomerResponse(UUID.randomUUID(), "ahmed@instance.com", "Ahmed", "Samir")
        );

        when(rateLimiterService.tryConsumeRegister(anyString())).thenReturn(true);

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"email": "ahmed@instance.com", "password": "password123", "firstName": "Ahmed", "lastName": "Samir"}
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ahmed@instance.com"));
    }

    @Test
    void register_shouldReturn409_whenEmailTaken() throws Exception {

        when(authService.register(any()))
                .thenThrow(new EmailAlreadyRegisteredException("ahmed@instance.com"));

        when(rateLimiterService.tryConsumeRegister(anyString())).thenReturn(true);

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"email": "ahmed@instance.com", "password": "password123", "firstName": "Ahmed", "lastName": "Samir"}
                                        """)
                )
                .andExpect(status().isConflict());
    }

    @Test
    void register_shouldReturn400_whenPasswordTooShort() throws Exception {

        when(rateLimiterService.tryConsumeRegister(anyString())).thenReturn(true);

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"email": "ahmed@instance.com", "password": "short", "firstName": "Ahmed", "lastName": "Samir"}
                                        """)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_shouldReturn429_whenRateLimitExceeded() throws Exception {

        when(rateLimiterService.tryConsumeRegister(anyString())).thenReturn(false);

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"email": "ahmed@instance.com", "password": "password123", "firstName": "Ahmed", "lastName": "Samir"}
                                        """)
                )
                .andExpect(status().isTooManyRequests());
    }



    @Test
    void login_shouldReturn429_whenIpRateLimitExceeded() throws Exception {

        when(rateLimiterService.tryConsumeLoginByIp(anyString())).thenReturn(false);

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {"email": "ahmed@instance.com", "password": "password123"}
                                    """)
                )
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void login_shouldReturn429_whenEmailRateLimitExceeded() throws Exception {

        when(rateLimiterService.tryConsumeLoginByIp(anyString())).thenReturn(true);
        when(rateLimiterService.tryConsumeLoginByEmail(anyString())).thenReturn(false);

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {"email": "ahmed@instance.com", "password": "password123"}
                                    """)
                )
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void login_shouldReturn429_whenIpAndEmailRateLimitExceeded() throws Exception {

        when(rateLimiterService.tryConsumeLoginByIp(anyString())).thenReturn(true);
        when(rateLimiterService.tryConsumeLoginByEmail(anyString())).thenReturn(true);
        when(rateLimiterService.tryConsumeLogin(anyString())).thenReturn(false);

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {"email": "ahmed@instance.com", "password": "password123"}
                                    """)
                )
                .andExpect(status().isTooManyRequests());
    }
}