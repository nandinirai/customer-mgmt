package com.nandinirai.customers.customer.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nandinirai.customers.customer.validation.BirthDate;
import com.nandinirai.customers.customer.validation.NameRules;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCustomerRequest(

        @NotBlank(message = "is required")
        @Size(max = NameRules.MAX_LENGTH, message = "must be at most 100 characters")
        @Pattern(regexp = NameRules.PATTERN, message = NameRules.MESSAGE)
        String firstName,

        @NotBlank(message = "is required")
        @Size(max = NameRules.MAX_LENGTH, message = "must be at most 100 characters")
        @Pattern(regexp = NameRules.PATTERN, message = NameRules.MESSAGE)
        String lastName,

        @NotNull(message = "is required")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @BirthDate
        LocalDate dateOfBirth) {

    public CreateCustomerRequest {
        firstName = normalise(firstName);
        lastName = normalise(lastName);
    }

    private static String normalise(String value) {
        return value == null ? null : value.strip().replaceAll("\\s{2,}", " ");
    }
}
