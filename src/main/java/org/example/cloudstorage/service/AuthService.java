package org.example.cloudstorage.service;

import io.minio.errors.MinioException;
import jakarta.servlet.http.HttpServletRequest;
import org.example.cloudstorage.dto.request.UserRequest;
import org.example.cloudstorage.dto.response.UserResponse;

public interface AuthService {

    UserResponse registration(UserRequest userRequest, HttpServletRequest request)
            throws MinioException;

    UserResponse login(UserRequest userRequest, HttpServletRequest request);
}
