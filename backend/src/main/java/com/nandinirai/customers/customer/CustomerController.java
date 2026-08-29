package com.nandinirai.customers.customer;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.nandinirai.customers.customer.dto.CreateCustomerRequest;
import com.nandinirai.customers.customer.dto.CustomerResponse;
import com.nandinirai.customers.customer.dto.PageResponse;
import com.nandinirai.customers.error.InvalidSortException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private static final Set<String> SORTABLE = Set.of("firstName", "lastName", "dateOfBirth", "createdAt");

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest request) {
        CustomerResponse created = service.create(request);
        URI location = UriComponentsBuilder.fromPath("/api/v1/customers/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public PageResponse<CustomerResponse> list(
            @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return service.find(q, toPageable(page, size, sort));
    }

    @GetMapping("/{id}")
    public CustomerResponse getById(@PathVariable UUID id) {
        return service.findById(id);
    }

    private Pageable toPageable(int page, int size, String sort) {
        return PageRequest.of(page, size, parseSort(sort));
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return DEFAULT_SORT;
        }
        List<Sort.Order> orders = java.util.Arrays.stream(sort.split(";"))
                .map(String::strip)
                .filter(part -> !part.isEmpty())
                .map(this::parseOrder)
                .toList();
        return orders.isEmpty() ? DEFAULT_SORT : Sort.by(orders);
    }

    private Sort.Order parseOrder(String part) {
        String[] tokens = part.split(",", 2);
        String property = tokens[0].strip();
        if (!SORTABLE.contains(property)) {
            throw new InvalidSortException(property, SORTABLE);
        }
        boolean descending = tokens.length > 1 && tokens[1].strip().equalsIgnoreCase("desc");
        Sort.Order order = descending ? Sort.Order.desc(property) : Sort.Order.asc(property);
        // Names sort by what a person reads, not by byte value: "de Silva"
        // should not land after "Zhang" because of the lowercase 'd'.
        return property.equals("dateOfBirth") || property.equals("createdAt") ? order : order.ignoreCase();
    }
}
