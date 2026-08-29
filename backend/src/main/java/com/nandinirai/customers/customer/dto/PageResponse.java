package com.nandinirai.customers.customer.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * A stable pagination envelope.
 *
 * <p>Serialising Spring's {@code PageImpl} directly leaks framework internals
 * into the public contract and its JSON shape is explicitly not guaranteed
 * across versions, so the API owns its own wrapper.
 */
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalItems,
        int totalPages) {

    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
