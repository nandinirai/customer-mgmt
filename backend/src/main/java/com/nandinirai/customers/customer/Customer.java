package com.nandinirai.customers.customer;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import jakarta.persistence.PrePersist;

/**
 * A customer record.
 *
 * <p>Date of birth is a {@link LocalDate}, not an instant: a birth date is a
 * calendar fact that does not shift with the reader's time zone. Age is
 * therefore derived on read rather than stored, since a stored age is wrong
 * from the day after it is written.
 */
@Entity
@Table(
        name = "customers",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_customers_identity",
                columnNames = {"first_name", "last_name", "date_of_birth"}),
        indexes = {
                @Index(name = "ix_customers_last_name", columnList = "last_name"),
                @Index(name = "ix_customers_created_at", columnList = "created_at")
        })
public class Customer {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void stampCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /** Required by JPA. Use {@link #of(String, String, LocalDate)} in application code. */
    protected Customer() {
    }

    private Customer(String firstName, String lastName, LocalDate dateOfBirth) {
        this.firstName = Objects.requireNonNull(firstName, "firstName");
        this.lastName = Objects.requireNonNull(lastName, "lastName");
        this.dateOfBirth = Objects.requireNonNull(dateOfBirth, "dateOfBirth");
    }

    public static Customer of(String firstName, String lastName, LocalDate dateOfBirth) {
        return new Customer(firstName, lastName, dateOfBirth);
    }

    public UUID getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Equality is by persistent identity only. Two unsaved instances are never
     * equal, which keeps identity-map semantics intact inside collections.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Customer customer)) {
            return false;
        }
        return id != null && id.equals(customer.id);
    }

    @Override
    public int hashCode() {
        return Customer.class.hashCode();
    }

    @Override
    public String toString() {
        return "Customer{id=%s, lastName='%s'}".formatted(id, lastName);
    }
}
