package com.nandinirai.customers.customer;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository repository;

    @BeforeEach
    void seed() {
        repository.saveAll(java.util.List.of(
                Customer.of("Jane", "Doe", LocalDate.of(1990, 4, 17)),
                Customer.of("John", "Doe", LocalDate.of(1985, 1, 2)),
                Customer.of("Aisha", "Khan", LocalDate.of(2000, 12, 31))));
        repository.flush();
    }

    @Test
    @DisplayName("duplicate detection ignores name casing")
    void duplicateCheckIsCaseInsensitive() {
        assertThat(repository.existsByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirth(
                "jane", "DOE", LocalDate.of(1990, 4, 17))).isTrue();

        assertThat(repository.existsByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirth(
                "Jane", "Doe", LocalDate.of(1991, 4, 17))).isFalse();
    }

    @Test
    @DisplayName("the unique constraint is enforced by the database, not only by the service")
    void databaseRejectsExactDuplicate() {
        repository.save(Customer.of("Jane", "Doe", LocalDate.of(1990, 4, 17)));

        assertThatThrownBy(() -> repository.flush())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("search matches either name, case-insensitively, on a partial term")
    void searchMatchesEitherName() {
        Page<Customer> byLastName = repository.searchByName("doe", PageRequest.of(0, 10));
        assertThat(byLastName.getTotalElements()).isEqualTo(2);

        Page<Customer> byFirstNameFragment = repository.searchByName("ais", PageRequest.of(0, 10));
        assertThat(byFirstNameFragment.getContent())
                .extracting(Customer::getLastName)
                .containsExactly("Khan");

        assertThat(repository.searchByName("nobody", PageRequest.of(0, 10)).getContent()).isEmpty();
    }

    @Test
    @DisplayName("paging returns the requested slice and the full total")
    void pagesResults() {
        Page<Customer> firstPage = repository.findAll(PageRequest.of(0, 2, Sort.by("lastName").ascending()));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
    }
}
