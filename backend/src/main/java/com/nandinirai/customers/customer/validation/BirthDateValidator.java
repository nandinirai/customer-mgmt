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
        if (value == null) {
            return true;
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return value.isBefore(today) && value.isAfter(today.minusYears(maxAgeYears));
    }
}
