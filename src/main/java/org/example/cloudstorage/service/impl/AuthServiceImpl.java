package org.example.cloudstorage.service.impl;

import io.minio.errors.MinioException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cloudstorage.dto.request.UserRequest;
import org.example.cloudstorage.dto.response.UserResponse;
import org.example.cloudstorage.service.AuthService;
import org.example.cloudstorage.service.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;

    @Override
    public UserResponse registration(UserRequest userRequest, HttpServletRequest request)
            throws MinioException {

        var userResponse = userService.registration(userRequest);

        createAndSaveSecurityContext(userRequest, request);

        log.info("User {} was registered", userResponse.username());
        return userResponse;
    }

    @Override
    public UserResponse login(UserRequest userRequest, HttpServletRequest request) {

        createAndSaveSecurityContext(userRequest, request);
        log.info("User with login {} was authorized", userRequest.username());
        return new UserResponse(userRequest.username());
    }

    private void createAndSaveSecurityContext(UserRequest userRequest, HttpServletRequest request) {
        Authentication authenticationRequest =
                UsernamePasswordAuthenticationToken.unauthenticated(
                        userRequest.username(), userRequest.password());

        Authentication authenticationResponse =
                authenticationManager.authenticate(authenticationRequest);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authenticationResponse);
        SecurityContextHolder.setContext(context);
        var session = request.getSession(true);
        session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, context);
    }
}
