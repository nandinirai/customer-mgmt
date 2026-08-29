package com.nandinirai.customers.error;

import java.net.URI;
import java.util.Comparator;
import java.util.List;

import com.nandinirai.customers.customer.CustomerNotFoundException;
import com.nandinirai.customers.customer.DuplicateCustomerException;

import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final URI VALIDATION_TYPE = URI.create("urn:problem-type:validation-error");
    private static final URI CONFLICT_TYPE = URI.create("urn:problem-type:duplicate-customer");
    private static final URI NOT_FOUND_TYPE = URI.create("urn:problem-type:customer-not-found");
    private static final URI MALFORMED_TYPE = URI.create("urn:problem-type:malformed-request");
    private static final URI INTERNAL_TYPE = URI.create("urn:problem-type:internal-error");

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        List<FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(FieldError::field))
                .toList();

        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, VALIDATION_TYPE,
                "Validation failed", "One or more fields are invalid.");
        problem.setProperty("errors", errors);

        log.debug("Rejected create payload: {}", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        List<String> messages = ex.getAllErrors().stream()
                .map(error -> error.getDefaultMessage() == null ? "is invalid" : error.getDefaultMessage())
                .toList();

        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, VALIDATION_TYPE,
                "Invalid request parameters", String.join("; ", messages));
        return ResponseEntity.badRequest().body(problem);
    }

    /**
     * Jackson could not build the payload — malformed JSON, or a date like
     * "1990-13-45". The client gets a usable hint; the parser's internal
     * message, which names Java types and byte offsets, stays in the log.
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        log.debug("Unreadable request body", ex);
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, MALFORMED_TYPE,
                "Malformed request",
                "The request body could not be parsed. Check that the JSON is well formed and that "
                        + "dateOfBirth is an ISO-8601 date such as 1990-04-17.");
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        List<FieldError> errors = ex.getConstraintViolations().stream()
                .map(violation -> new FieldError(
                        lastNode(violation.getPropertyPath().toString()), violation.getMessage()))
                .sorted(Comparator.comparing(FieldError::field))
                .toList();

        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, VALIDATION_TYPE,
                "Invalid request parameters", "One or more parameters are invalid.");
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String name = ex.getName();
        String detail = "id".equals(name)
                ? "The customer id must be a UUID."
                : "The value supplied for '%s' is not valid.".formatted(name);
        return problem(HttpStatus.BAD_REQUEST, MALFORMED_TYPE, "Invalid request parameters", detail);
    }

    @ExceptionHandler(InvalidSortException.class)
    public ProblemDetail handleInvalidSort(InvalidSortException ex) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, VALIDATION_TYPE,
                "Invalid sort", ex.getMessage());
        problem.setProperty("allowedSortFields", ex.getAllowed());
        return problem;
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    public ProblemDetail handleNotFound(CustomerNotFoundException ex) {
        ProblemDetail problem = problem(HttpStatus.NOT_FOUND, NOT_FOUND_TYPE,
                "Customer not found", "No customer exists with that id.");
        problem.setProperty("customerId", ex.getId());
        return problem;
    }

    @ExceptionHandler(DuplicateCustomerException.class)
    public ProblemDetail handleDuplicate(DuplicateCustomerException ex) {
        return problem(HttpStatus.CONFLICT, CONFLICT_TYPE, "Duplicate customer", ex.getMessage());
    }

    /**
     * A constraint violation that escaped the service layer. It still means the
     * write conflicted with existing data, so the caller sees 409 — but it is
     * worth a warning, because reaching here means a check was missed.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation reached the web layer", ex);
        return problem(HttpStatus.CONFLICT, CONFLICT_TYPE, "Conflict",
                "The record conflicts with data that already exists.");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_TYPE, "Internal server error",
                "Something went wrong. Please try again.");
    }

    private static ProblemDetail problem(HttpStatus status, URI type, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(type);
        problem.setTitle(title);
        return problem;
    }

    private static String lastNode(String propertyPath) {
        int index = propertyPath.lastIndexOf('.');
        return index < 0 ? propertyPath : propertyPath.substring(index + 1);
    }
}
