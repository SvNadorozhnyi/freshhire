package io.github.svnadorozhnyi.freshhire.common;

import io.github.svnadorozhnyi.freshhire.company.CompanyNotFoundException;
import io.github.svnadorozhnyi.freshhire.company.DuplicateCompanyException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.databind.exc.InvalidFormatException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ValidationError> errors = ex.getBindingResult().getAllErrors().stream()
                .map(GlobalExceptionHandler::toValidationError)
                .toList();

        ProblemDetail problem = ex.getBody();
        problem.setProperty("errors", errors);

        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Malformed JSON request body");
        if (ex.getCause() instanceof InvalidFormatException invalidFormat
                && invalidFormat.getTargetType() != null
                && invalidFormat.getTargetType().isEnum()) {
            problem.setDetail("Invalid request content.");
            problem.setProperty(
                    "errors",
                    List.of(new ValidationError(
                            fieldPath(invalidFormat), "must be one of: " + enumValues(invalidFormat.getTargetType()))));
        }
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @ExceptionHandler(CompanyNotFoundException.class)
    public ProblemDetail handleCompanyNotFound(CompanyNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicateCompanyException.class)
    public ProblemDetail handleDuplicateCompany(DuplicateCompanyException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ProblemDetail handleUnknownProperty(PropertyReferenceException ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Unknown sort property: " + ex.getPropertyName());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private static ValidationError toValidationError(ObjectError error) {
        if (error instanceof FieldError fieldError) {
            return new ValidationError(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return new ValidationError(error.getObjectName(), error.getDefaultMessage());
    }

    private static String fieldPath(InvalidFormatException ex) {
        return ex.getPath().stream()
                .map(reference -> reference.getPropertyName() != null
                        ? reference.getPropertyName()
                        : "[" + reference.getIndex() + "]")
                .collect(Collectors.joining("."));
    }

    private static String enumValues(Class<?> enumType) {
        return Arrays.stream(enumType.getEnumConstants()).map(Object::toString).collect(Collectors.joining(", "));
    }

    record ValidationError(String field, String message) {}
}
