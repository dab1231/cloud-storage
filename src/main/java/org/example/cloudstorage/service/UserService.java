package org.example.cloudstorage.service;

import io.minio.errors.MinioException;
import org.example.cloudstorage.dto.request.UserRequest;
import org.example.cloudstorage.dto.response.UserResponse;

public interface UserService {

    UserResponse registration(UserRequest userRequest) throws MinioException;
}
