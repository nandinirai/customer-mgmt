package com.nandinirai.customers.customer.validation;

import java.time.LocalDate;
import java.time.ZoneOffset;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class BirthDateValidator implements ConstraintValidator<BirthDate, LocalDate> {

    private int maxAgeYears;

    @Override
    public void initialize(BirthDate constraint) {
        this.maxAgeYears = constraint.maxAgeYears();
    }

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        // Null is @NotNull's job, not ours — one constraint, one message.
        if (value == null) {
            return true;
        }
        // UTC rather than the server default: whichever zone we pick, someone
        // is a day ahead. UTC at least makes the behaviour explicit and stable
        // across deployments, and the boundary only matters for babies born today.
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return value.isBefore(today) && value.isAfter(today.minusYears(maxAgeYears));
    }
}
