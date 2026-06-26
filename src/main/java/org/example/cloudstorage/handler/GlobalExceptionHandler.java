package org.example.cloudstorage.handler;

import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import lombok.extern.slf4j.Slf4j;
import org.example.cloudstorage.dto.response.ErrorResponse;
import org.example.cloudstorage.exception.*;
import org.jspecify.annotations.NonNull;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Objects;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({
            UserAlreadyExistsException.class,
            ResourceAlreadyExistsException.class,
    })
    public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException(Exception e) {

        var errorMessage = e.getMessage();
        log.warn(errorMessage);
        return new ResponseEntity<>(new ErrorResponse(errorMessage), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException e) {

        var errorMessage = e.getMessage();
        log.warn(errorMessage);
        return new ResponseEntity<>(new ErrorResponse(errorMessage), HttpStatus.UNAUTHORIZED);
    }

    @Override
    public ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {
        List<String> errorMessages =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(DefaultMessageSourceResolvable::getDefaultMessage)
                        .toList();

        String errorMessage = String.join("; ", errorMessages);
        for (String message : errorMessages) {
            log.warn(message);
        }
        return new ResponseEntity<>(new ErrorResponse(errorMessage), HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {

        var errorMessage = ex.getMessage();
        log.warn(errorMessage);
        return new ResponseEntity<>(new ErrorResponse(errorMessage), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidBodyException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBodyException(InvalidBodyException e) {

        var errorMessage = e.getMessage();
        log.warn(errorMessage);
        return new ResponseEntity<>(new ErrorResponse(errorMessage), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidPathException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPathException(InvalidPathException e) {

        var errorMessage = e.getMessage();
        log.warn(errorMessage);
        return new ResponseEntity<>(new ErrorResponse(errorMessage), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ErrorResponse> handleErrorResponseException(ErrorResponseException e) {

        if (Objects.equals(e.errorResponse().code(), "NoSuchKey")) {

            var errorMessage = e.getMessage();
            log.warn(errorMessage);
            return new ResponseEntity<>(new ErrorResponse(errorMessage), HttpStatus.NOT_FOUND);
        } else {

            var errorMessage = e.getMessage();
            log.warn(errorMessage);
            return new ResponseEntity<>(
                    new ErrorResponse(errorMessage), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException e) {

        var errorMessage = e.getMessage();
        log.warn(errorMessage);
        return new ResponseEntity<>(new ErrorResponse(errorMessage), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MinioException.class)
    public ResponseEntity<ErrorResponse> handleMinioException(MinioException e) {

        var errorMessage = e.getMessage();
        log.warn(errorMessage);
        return new ResponseEntity<>(new ErrorResponse(errorMessage), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {

        var errorMessage = e.getMessage();
        log.warn(errorMessage);
        return new ResponseEntity<>(new ErrorResponse(errorMessage), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
