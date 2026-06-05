package org.example.cloudstorage.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.cloudstorage.dto.request.UserRequest;
import org.example.cloudstorage.dto.response.UserResponse;
import org.example.cloudstorage.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/sign-up")
    public ResponseEntity<UserResponse> registration(@RequestBody UserRequest userRequest,
                                                     HttpServletRequest request) {

        var user = userService.registration(userRequest);
        Authentication authenticationRequest = UsernamePasswordAuthenticationToken.unauthenticated(
                userRequest.username(), userRequest.password());

        Authentication authenticationResponse = authenticationManager.authenticate(authenticationRequest);
        SecurityContextHolder.getContext().setAuthentication(authenticationResponse);
        request.getSession(true);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UserResponse(user.username()));
    }

    @PostMapping("/sign-in")
    public ResponseEntity<UserResponse> login(@RequestBody UserRequest userRequest, HttpServletRequest request) {

        Authentication authenticationRequest = UsernamePasswordAuthenticationToken.unauthenticated(
                userRequest.username(), userRequest.password());

        Authentication authenticationResponse = authenticationManager.authenticate(authenticationRequest);
        SecurityContextHolder.getContext().setAuthentication(authenticationResponse);
        request.getSession(true);

        return ResponseEntity.status(HttpStatus.OK)
                .body(new UserResponse(authenticationResponse.getName()));
    }
}
