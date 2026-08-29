package com.nandinirai.customers.customer;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;

import com.nandinirai.customers.customer.dto.CreateCustomerRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The validation rules on their own, without a Spring context. Fast enough to
 * cover the name edge cases properly, which is where a naive regex does real
 * damage by rejecting people who exist.
 */
class CreateCustomerRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @ParameterizedTest(name = "accepts the name \"{0}\"")
    @ValueSource(strings = {"Jane", "O'Connor", "Anne-Marie", "Zoë", "李", "van der Berg", "St. John", "O\u2019Neill"})
    @DisplayName("real names are accepted, including non-ASCII and punctuated ones")
    void acceptsRealNames(String name) {
        assertThat(violations(new CreateCustomerRequest(name, name, LocalDate.of(1990, 4, 17)))).isEmpty();
    }

    @ParameterizedTest(name = "rejects the name \"{0}\"")
    @ValueSource(strings = {"", "   ", "Jane123", "<script>", "Jane@Doe", "-Jane"})
    @DisplayName("empty, numeric and markup-like values are rejected")
    void rejectsInvalidNames(String name) {
        assertThat(violations(new CreateCustomerRequest(name, "Doe", LocalDate.of(1990, 4, 17))))
                .isNotEmpty();
    }

    @Test
    @DisplayName("a name longer than 100 characters is rejected")
    void rejectsOverlongName() {
        String tooLong = "A".repeat(101);
        assertThat(violations(new CreateCustomerRequest(tooLong, "Doe", LocalDate.of(1990, 4, 17))))
                .isNotEmpty();
    }

    @Test
    @DisplayName("interior whitespace is collapsed and padding stripped on construction")
    void normalisesWhitespace() {
        CreateCustomerRequest request = new CreateCustomerRequest("  Anne   Marie ", " van   Berg  ", LocalDate.of(1990, 4, 17));

        assertThat(request.firstName()).isEqualTo("Anne Marie");
        assertThat(request.lastName()).isEqualTo("van Berg");
    }

    @Test
    @DisplayName("a missing date of birth reports exactly one message, not two")
    void reportsMissingDateOnce() {
        Set<ConstraintViolation<CreateCustomerRequest>> violations =
                violations(new CreateCustomerRequest("Jane", "Doe", null));

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("is required");
    }

    @Test
    @DisplayName("today is not a valid date of birth, yesterday is")
    void treatsTodayAsInvalid() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        assertThat(violations(new CreateCustomerRequest("Jane", "Doe", today))).isNotEmpty();
        assertThat(violations(new CreateCustomerRequest("Jane", "Doe", today.minusDays(1)))).isEmpty();
    }

    @Test
    @DisplayName("an implausibly old date of birth is rejected as a typo")
    void rejectsImplausiblyOldDate() {
        assertThat(violations(new CreateCustomerRequest("Jane", "Doe", LocalDate.of(1850, 1, 1))))
                .isNotEmpty();
    }

    private static Set<ConstraintViolation<CreateCustomerRequest>> violations(CreateCustomerRequest request) {
        return validator.validate(request);
    }
}
