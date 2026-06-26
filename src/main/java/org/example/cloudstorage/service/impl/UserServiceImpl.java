package org.example.cloudstorage.service.impl;

import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cloudstorage.dto.request.UserRequest;
import org.example.cloudstorage.dto.response.UserResponse;
import org.example.cloudstorage.entity.Role;
import org.example.cloudstorage.entity.User;
import org.example.cloudstorage.exception.UserAlreadyExistsException;
import org.example.cloudstorage.repository.UserRepository;
import org.example.cloudstorage.service.MinioService;
import org.example.cloudstorage.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MinioService minioService;

    @Override
    public UserResponse registration(UserRequest userRequest) throws MinioException {

        userRepository
                .findByUsername(userRequest.username())
                .ifPresent(
                        user -> {
                            throw new UserAlreadyExistsException(
                                    "User with username " + userRequest.username() + " already exists");
                        });

        var user =
                User.builder()
                        .username(userRequest.username())
                        .password(passwordEncoder.encode(userRequest.password()))
                        .role(Role.USER)
                        .build();

        var savedUser = userRepository.save(user);

        minioService.createUserDirectory(minioService.getUserDirectoryName(savedUser.getId()));

        return UserResponse.builder().username(savedUser.getUsername()).build();
    }
}
