package com.bookingeventflow.customer.controller;

import com.bookingeventflow.customer.presentation.response.CustomerResponse;
import com.bookingeventflow.customer.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void getCurrentCustomer_shouldReturn200_whenAuthenticated() throws Exception {

        UUID customerId = UUID.randomUUID();

        when(authService.getCurrentCustomer(any()))
                .thenReturn(new CustomerResponse(customerId, "ahmed@instance.com", "Ahmed", "Samir"));

        mockMvc.perform(
                        get("/api/v1/customers/me")
                                .with(jwt().jwt(builder -> builder.subject(customerId.toString())))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ahmed@instance.com"));
    }

    @Test
    void getCurrentCustomer_shouldReturn401_whenNoToken() throws Exception {

        mockMvc.perform(get("/api/v1/customers/me"))
                .andExpect(status().isUnauthorized());
    }
}