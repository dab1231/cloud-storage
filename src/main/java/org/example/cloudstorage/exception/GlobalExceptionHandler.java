package org.example.cloudstorage.exception;

import org.example.cloudstorage.dto.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException(UserAlreadyExistsException e) {

        var errorMessage = e.getMessage();
        return new ResponseEntity<>(new ErrorResponse(errorMessage), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException e) {

        var errorMessage = e.getMessage();
        return new ResponseEntity<>(new ErrorResponse(errorMessage), HttpStatus.UNAUTHORIZED);
    }


}
