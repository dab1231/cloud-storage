package org.example.cloudstorage.controller;

import io.minio.errors.MinioException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.cloudstorage.controller.api.AuthControllerApi;
import org.example.cloudstorage.dto.request.UserRequest;
import org.example.cloudstorage.dto.response.UserResponse;
import org.example.cloudstorage.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthControllerApi {

    private final AuthService authService;

    @Override
    public ResponseEntity<UserResponse> registration(
            UserRequest userRequest, HttpServletRequest request) throws MinioException {

        var userResponse = authService.registration(userRequest, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
    }

    @Override
    public ResponseEntity<UserResponse> login(UserRequest userRequest, HttpServletRequest request) {

        var userResponse = authService.login(userRequest, request);

        return ResponseEntity.status(HttpStatus.OK).body(userResponse);
    }
}
