package com.nandinirai.customers.customer;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nandinirai.customers.customer.dto.CreateCustomerRequest;
import com.nandinirai.customers.customer.dto.CustomerResponse;
import com.nandinirai.customers.customer.dto.PageResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the HTTP contract only: status codes, headers and error shape. The
 * service is mocked, because what is under test here is the edge, not the rules.
 */
@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    private static final UUID ID = UUID.fromString("6a3f0c4e-6f4a-4a6f-9c1e-2b3d4e5f6a7b");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerService service;

    @Test
    @DisplayName("POST returns 201 with a Location header pointing at the new record")
    void createsCustomer() throws Exception {
        CustomerResponse response = new CustomerResponse(
                ID, "Jane", "Doe", LocalDate.of(1990, 4, 17), 35, Instant.parse("2025-06-15T09:00:00Z"));
        when(service.create(any(CreateCustomerRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Jane", "Doe", "1990-04-17")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/customers/" + ID))
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.age").value(35));
    }

    @Test
    @DisplayName("a blank name is rejected with a per-field error the UI can bind to")
    void rejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("  ", "Doe", "1990-04-17")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("firstName"));
    }

    @ParameterizedTest(name = "rejects date of birth {0}")
    @ValueSource(strings = {"2099-01-01", "1800-01-01"})
    @DisplayName("a future or implausibly distant birth date is rejected")
    void rejectsImplausibleBirthDate(String dateOfBirth) throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Jane", "Doe", dateOfBirth)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("dateOfBirth"));
    }

    @Test
    @DisplayName("an unparseable date is a malformed request, not a server error")
    void rejectsMalformedDate() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Jane", "Doe", "17/04/1990")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Malformed request"));
    }

    @Test
    @DisplayName("a duplicate surfaces as 409, not 400 or 500")
    void reportsDuplicateAsConflict() throws Exception {
        when(service.create(any(CreateCustomerRequest.class)))
                .thenThrow(new DuplicateCustomerException("Jane", "Doe"));

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Jane", "Doe", "1990-04-17")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate customer"));
    }

    @Test
    @DisplayName("GET by unknown id returns 404 with a problem document")
    void returnsNotFound() throws Exception {
        when(service.findById(ID)).thenThrow(new CustomerNotFoundException(ID));

        mockMvc.perform(get("/api/v1/customers/{id}", ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Customer not found"));
    }

    @Test
    @DisplayName("GET by a non-UUID id returns 400 rather than leaking a conversion failure")
    void rejectsNonUuidId() throws Exception {
        mockMvc.perform(get("/api/v1/customers/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("The customer id must be a UUID."));
    }

    @Test
    @DisplayName("GET returns a paging envelope owned by this API")
    void listsCustomers() throws Exception {
        CustomerResponse response = new CustomerResponse(
                ID, "Jane", "Doe", LocalDate.of(1990, 4, 17), 35, Instant.parse("2025-06-15T09:00:00Z"));
        when(service.find(isNull(), any())).thenReturn(
                new PageResponse<>(java.util.List.of(response), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].lastName").value("Doe"))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    @DisplayName("an unsupported sort field is refused with the allowed set")
    void rejectsUnknownSortField() throws Exception {
        mockMvc.perform(get("/api/v1/customers").param("sort", "password,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid sort"))
                .andExpect(jsonPath("$.allowedSortFields").isArray());
    }

    @Test
    @DisplayName("an out-of-range page size is refused")
    void rejectsOversizedPage() throws Exception {
        mockMvc.perform(get("/api/v1/customers").param("size", "5000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("search term is passed through to the service")
    void passesSearchTerm() throws Exception {
        when(service.find(eq("doe"), any())).thenReturn(new PageResponse<>(java.util.List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/customers").param("q", "doe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    private String body(String firstName, String lastName, String dateOfBirth) throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of(
                "firstName", firstName,
                "lastName", lastName,
                "dateOfBirth", dateOfBirth));
    }
}
