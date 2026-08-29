package com.nandinirai.customers.customer;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import com.nandinirai.customers.customer.dto.CreateCustomerRequest;
import com.nandinirai.customers.customer.dto.CustomerResponse;
import com.nandinirai.customers.customer.dto.PageResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository repository;
    private final Clock clock;

    public CustomerService(CustomerRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public CustomerResponse create(CreateCustomerRequest request) {
        if (repository.existsByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirth(
                request.firstName(), request.lastName(), request.dateOfBirth())) {
            throw new DuplicateCustomerException(request.firstName(), request.lastName());
        }

        Customer customer = Customer.of(request.firstName(), request.lastName(), request.dateOfBirth());
        try {
            Customer saved = repository.saveAndFlush(customer);
            log.info("Created customer {}", saved.getId());
            return toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            log.debug("Concurrent create lost the race to the unique constraint", ex);
            throw new DuplicateCustomerException(request.firstName(), request.lastName());
        }
    }

    public CustomerResponse findById(UUID id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new CustomerNotFoundException(id));
    }

    /**
     * @param searchTerm optional case-insensitive substring matched against
     *                   either name; blank or null returns everything
     */
    public PageResponse<CustomerResponse> find(String searchTerm, Pageable pageable) {
        Page<Customer> page = (searchTerm == null || searchTerm.isBlank())
                ? repository.findAll(pageable)
                : repository.searchByName(searchTerm.strip(), pageable);
        return PageResponse.from(page, this::toResponse);
    }

    private CustomerResponse toResponse(Customer customer) {
        return CustomerResponse.from(customer, LocalDate.now(clock));
    }
}
