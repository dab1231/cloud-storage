package org.example.cloudstorage.service.impl;

import io.minio.errors.MinioException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cloudstorage.dto.request.UserRequest;
import org.example.cloudstorage.dto.response.UserResponse;
import org.example.cloudstorage.helper.SecurityContextHelper;
import org.example.cloudstorage.service.AuthService;
import org.example.cloudstorage.service.UserService;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final SecurityContextHelper securityContextHelper;

    @Override
    public UserResponse register(UserRequest userRequest, HttpServletRequest request)
            throws MinioException {

        var userResponse = userService.saveUserAndCreateDirectory(userRequest);

        securityContextHelper.createAndSaveSecurityContext(userRequest, request);
        log.info("User {} was registered", userResponse.username());
        return userResponse;
    }

    @Override
    public UserResponse login(UserRequest userRequest, HttpServletRequest request) {

        securityContextHelper.createAndSaveSecurityContext(userRequest, request);
        log.info("User with login {} was authorized", userRequest.username());
        return new UserResponse(userRequest.username());
    }
}
