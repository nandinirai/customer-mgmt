package com.nandinirai.customers.customer;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import com.nandinirai.customers.customer.dto.CreateCustomerRequest;
import com.nandinirai.customers.customer.dto.CustomerResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    /** Pinned so age assertions do not change meaning on someone's birthday. */
    private final Clock clock = Clock.fixed(Instant.parse("2025-06-15T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    private CustomerRepository repository;

    private CustomerService service;

    private CustomerService service() {
        if (service == null) {
            service = new CustomerService(repository, clock);
        }
        return service;
    }

    @Test
    @DisplayName("stores the customer and returns it with a derived age")
    void createsCustomer() {
        CreateCustomerRequest request = new CreateCustomerRequest("Jane", "Doe", LocalDate.of(1990, 4, 17));
        when(repository.existsByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirth(
                "Jane", "Doe", LocalDate.of(1990, 4, 17))).thenReturn(false);
        when(repository.saveAndFlush(any(Customer.class))).thenAnswer(invocation -> persisted(invocation.getArgument(0)));

        CustomerResponse response = service().create(request);

        assertThat(response.firstName()).isEqualTo("Jane");
        assertThat(response.lastName()).isEqualTo("Doe");
        assertThat(response.dateOfBirth()).isEqualTo(LocalDate.of(1990, 4, 17));
        assertThat(response.age()).isEqualTo(35);
        assertThat(response.id()).isNotNull();
    }

    @ParameterizedTest(name = "born {0} is {1} on 2025-06-15")
    @CsvSource({
            "2025-06-14, 0",   // born yesterday
            "2005-06-15, 20",  // birthday is today
            "2005-06-16, 19",  // birthday is tomorrow
            "2004-02-29, 21"   // leap day, in a non-leap year
    })
    @DisplayName("age is calculated on whole elapsed years, including the awkward dates")
    void calculatesAge(LocalDate dateOfBirth, int expectedAge) {
        Customer customer = persisted(Customer.of("Test", "Person", dateOfBirth));

        CustomerResponse response = CustomerResponse.from(customer, LocalDate.now(clock));

        assertThat(response.age()).isEqualTo(expectedAge);
    }

    @Test
    @DisplayName("rejects a customer that already exists rather than creating a second record")
    void rejectsDuplicate() {
        CreateCustomerRequest request = new CreateCustomerRequest("Jane", "Doe", LocalDate.of(1990, 4, 17));
        when(repository.existsByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirth(
                "Jane", "Doe", LocalDate.of(1990, 4, 17))).thenReturn(true);

        assertThatThrownBy(() -> service().create(request))
                .isInstanceOf(DuplicateCustomerException.class)
                .hasMessageContaining("Jane Doe");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("translates a lost concurrent insert into the same conflict the pre-check gives")
    void translatesIntegrityViolationToDuplicate() {
        CreateCustomerRequest request = new CreateCustomerRequest("Jane", "Doe", LocalDate.of(1990, 4, 17));
        when(repository.existsByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirth(
                any(), any(), any())).thenReturn(false);
        when(repository.saveAndFlush(any(Customer.class)))
                .thenThrow(new DataIntegrityViolationException("uk_customers_identity"));

        assertThatThrownBy(() -> service().create(request))
                .isInstanceOf(DuplicateCustomerException.class);
    }

    @Test
    @DisplayName("reports a missing customer as not found rather than returning an empty body")
    void throwsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().findById(id))
                .isInstanceOf(CustomerNotFoundException.class);
    }

    /** Simulates what JPA fills in on flush, so mapping can be asserted. */
    private static Customer persisted(Customer customer) {
        ReflectionTestUtils.setField(customer, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(customer, "createdAt", Instant.parse("2025-06-15T09:00:00Z"));
        return customer;
    }
}
