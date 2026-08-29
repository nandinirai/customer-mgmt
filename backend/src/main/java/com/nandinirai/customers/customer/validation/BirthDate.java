package com.nandinirai.customers.customer.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * A plausible human birth date: in the past, and not implausibly long ago.
 *
 * <p>{@code @Past} alone accepts the year 1200, which is a data-entry mistake
 * rather than a customer. The upper bound is deliberately generous — the oldest
 * verified human age is around 122 — so it rejects typos without rejecting
 * anyone real.
 */
@Documented
@Constraint(validatedBy = BirthDateValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER,
        ElementType.RECORD_COMPONENT, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface BirthDate {

    String message() default "must be in the past and within the last 130 years";

    int maxAgeYears() default 130;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
