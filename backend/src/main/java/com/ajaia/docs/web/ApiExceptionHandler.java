package com.ajaia.docs.web;

import com.ajaia.docs.web.dto.ApiError;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Every API failure leaves through here so the client always gets the same
 * JSON shape and never gets a stack trace.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> onValidationFailure(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        String message = fieldErrors.values().stream().findFirst().orElse("Check the values you entered");
        return build(HttpStatus.BAD_REQUEST, message, fieldErrors);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> onMissingParameter(MissingServletRequestParameterException e) {
        return build(HttpStatus.BAD_REQUEST, "Missing required value: " + e.getParameterName(), null);
    }

    /** An upload posted without the file part, or with a broken multipart body. */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiError> onMissingFilePart(MissingServletRequestPartException e) {
        return build(HttpStatus.BAD_REQUEST, "Missing required value: " + e.getRequestPartName(), null);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiError> onBrokenUpload(MultipartException e) {
        return build(HttpStatus.BAD_REQUEST, "The upload could not be read. Try selecting the file again", null);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> onBadRequest(BadRequestException e) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage(), null);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> onUnauthorized(UnauthorizedException e) {
        return build(HttpStatus.UNAUTHORIZED, e.getMessage(), null);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ApiError> onTooManyRequests(TooManyRequestsException e) {
        return build(HttpStatus.TOO_MANY_REQUESTS, e.getMessage(), null);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> onForbidden(ForbiddenException e) {
        return build(HttpStatus.FORBIDDEN, e.getMessage(), null);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> onNotFound(NotFoundException e) {
        return build(HttpStatus.NOT_FOUND, e.getMessage(), null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> onUploadTooLarge(MaxUploadSizeExceededException e) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "That file is too large to upload", null);
    }

    /**
     * A body that is not valid JSON, or that carries a value the target type
     * cannot hold, such as a role outside the enum. Both are the caller getting
     * the request wrong, not the server failing.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> onUnreadableBody(HttpMessageNotReadableException e) {
        return build(HttpStatus.BAD_REQUEST, describeUnreadableBody(e), null);
    }

    /** A path variable that cannot be converted, for example /api/documents/abc. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> onBadPathValue(MethodArgumentTypeMismatchException e) {
        return build(HttpStatus.BAD_REQUEST, "'" + e.getName() + "' is not a valid value", null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> onWrongContentType(HttpMediaTypeNotSupportedException e) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "This endpoint does not accept that content type", null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> onWrongMethod(HttpRequestMethodNotSupportedException e) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, e.getMethod() + " is not allowed here", null);
    }

    /** An API path that no controller handles. */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiError> onUnknownEndpoint(Exception e) {
        return build(HttpStatus.NOT_FOUND, "No such endpoint", null);
    }

    /**
     * Usually two requests racing to create the same share. The unique constraint
     * is the thing that actually decides, and losing that race is a conflict
     * rather than a server fault.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> onConstraintViolation(DataIntegrityViolationException e) {
        log.warn("Database constraint rejected a request: {}", e.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "That change conflicts with the current state. Reload and try again", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> onUnexpected(Exception e) {
        // The details stay in the server log. The client only sees that
        // something went wrong.
        log.error("Unhandled error while serving a request", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong on our side", null);
    }

    /**
     * Names the offending field when Jackson can identify it, because "role must
     * be one of VIEWER, EDITOR" is far more useful than "malformed request".
     */
    private String describeUnreadableBody(HttpMessageNotReadableException e) {
        if (e.getCause() instanceof InvalidFormatException invalid) {
            Class<?> target = invalid.getTargetType();
            if (target != null && target.isEnum()) {
                String allowed = Arrays.stream(target.getEnumConstants())
                        .map(String::valueOf)
                        .collect(Collectors.joining(", "));
                return "'" + invalid.getValue() + "' is not valid here. Expected one of: " + allowed;
            }
        }
        return "The request body could not be read. Check that it is valid JSON";
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, Map<String, String> fieldErrors) {
        String safeMessage = message == null || message.isBlank() ? status.getReasonPhrase() : message;
        return ResponseEntity.status(status).body(new ApiError(status.value(), safeMessage, fieldErrors));
    }
}
