package com.nandinirai.customers.customer;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {


    boolean existsByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirth(
            String firstName, String lastName, LocalDate dateOfBirth);

    @Query("""
            select c from Customer c
            where lower(c.firstName) like lower(concat('%', :term, '%'))
               or lower(c.lastName)  like lower(concat('%', :term, '%'))
            """)
    Page<Customer> searchByName(@Param("term") String term, Pageable pageable);
}
