package com.nandinirai.customers.customer;

import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end through the real web stack, the real service and a real H2
 * database. These are the tests that would have caught a wiring or mapping
 * mistake that every mocked layer above happily agreed on.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CustomerApiIntegrationTest {

    private static final String PATH = "/api/v1/customers";

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private CustomerRepository repository;

    @BeforeEach
    void clearDatabase() {
        // The in-memory database outlives a single test method, so state is
        // reset explicitly rather than relying on test ordering.
        repository.deleteAll();
    }

    @Test
    @DisplayName("a created customer can be fetched back from its Location header")
    void createThenReadBack() {
        LocalDate dateOfBirth = LocalDate.now(ZoneOffset.UTC).minusYears(30);

        ResponseEntity<Map> created = rest.postForEntity(PATH, payload("Jane", "Doe", dateOfBirth), Map.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        URI location = created.getHeaders().getLocation();
        assertThat(location).isNotNull();
        assertThat(created.getBody()).containsEntry("age", 30);

        ResponseEntity<Map> fetched = rest.getForEntity(location, Map.class);

        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody())
                .containsEntry("firstName", "Jane")
                .containsEntry("lastName", "Doe")
                .containsEntry("dateOfBirth", dateOfBirth.toString());
    }

    @Test
    @DisplayName("surrounding whitespace is normalised before the record is stored")
    void normalisesWhitespace() {
        rest.postForEntity(PATH, payload("  Jane  ", "  van   Doe ", LocalDate.of(1990, 4, 17)), Map.class);

        ResponseEntity<Map> list = rest.getForEntity(PATH, Map.class);
        Map<?, ?> first = (Map<?, ?>) ((java.util.List<?>) list.getBody().get("items")).get(0);

        assertThat(first.get("firstName")).isEqualTo("Jane");
        assertThat(first.get("lastName")).isEqualTo("van Doe");
    }

    @Test
    @DisplayName("posting the same person twice returns 409 and leaves one record")
    void rejectsDuplicate() {
        Map<String, Object> payload = payload("Jane", "Doe", LocalDate.of(1990, 4, 17));

        assertThat(rest.postForEntity(PATH, payload, Map.class).getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<Map> second = rest.postForEntity(PATH, payload, Map.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(second.getBody()).containsEntry("title", "Duplicate customer");
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("duplicate detection ignores casing end to end")
    void rejectsDuplicateIgnoringCase() {
        rest.postForEntity(PATH, payload("Jane", "Doe", LocalDate.of(1990, 4, 17)), Map.class);

        ResponseEntity<Map> second =
                rest.postForEntity(PATH, payload("jane", "DOE", LocalDate.of(1990, 4, 17)), Map.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("validation failures come back as a problem document with field errors")
    void reportsValidationErrors() {
        ResponseEntity<Map> response =
                rest.postForEntity(PATH, payload("", "Doe", LocalDate.of(1990, 4, 17)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsKey("errors");
    }

    @Test
    @DisplayName("search and paging work against real data")
    void searchesAndPages() {
        rest.postForEntity(PATH, payload("Jane", "Doe", LocalDate.of(1990, 4, 17)), Map.class);
        rest.postForEntity(PATH, payload("John", "Doe", LocalDate.of(1985, 1, 2)), Map.class);
        rest.postForEntity(PATH, payload("Aisha", "Khan", LocalDate.of(2000, 12, 31)), Map.class);

        ResponseEntity<Map> search = rest.getForEntity(PATH + "?q=doe", Map.class);
        assertThat(search.getBody()).containsEntry("totalItems", 2);

        ResponseEntity<Map> firstPage = rest.getForEntity(PATH + "?size=2&sort=lastName,asc", Map.class);
        assertThat(firstPage.getBody()).containsEntry("totalItems", 3).containsEntry("totalPages", 2);
        assertThat((java.util.List<?>) firstPage.getBody().get("items")).hasSize(2);
    }

    @Test
    @DisplayName("an unknown id returns 404")
    void unknownIdReturnsNotFound() {
        ResponseEntity<Map> response =
                rest.getForEntity(PATH + "/" + java.util.UUID.randomUUID(), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private static Map<String, Object> payload(String firstName, String lastName, LocalDate dateOfBirth) {
        return Map.of(
                "firstName", firstName,
                "lastName", lastName,
                "dateOfBirth", dateOfBirth.toString());
    }
}
