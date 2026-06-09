package org.example.cloudstorage.integration.service;

import org.example.cloudstorage.dto.request.UserRequest;
import org.example.cloudstorage.exception.UserAlreadyExistsException;
import org.example.cloudstorage.repository.UserRepository;
import org.example.cloudstorage.service.UserService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserServiceTest {

    final String USERNAME = "test";
    final String PASSWORD = "test";

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserService userService;

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        "postgres:16"
    );

    @BeforeAll
    static void beforeAll() {
        postgres.start();
    }

    @AfterAll
    static void afterAll() {
        postgres.stop();
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry dynamicPropertyRegistry) {
        dynamicPropertyRegistry.add("spring.datasource.url", postgres::getJdbcUrl);
        dynamicPropertyRegistry.add("spring.datasource.username", postgres::getUsername);
        dynamicPropertyRegistry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void registration() {
        var user = new UserRequest(USERNAME, PASSWORD);

        var userResponse = userService.registration(user);

        var actualUser = userRepository.findByUsername(userResponse.username());

        assertTrue(actualUser.isPresent());
        actualUser.ifPresent(actual -> assertEquals(USERNAME, actual.getUsername()));
    }

    @Test
    void throwExceptionIfUserAlreadyExists() {

        var user = new UserRequest(USERNAME, PASSWORD);
        userService.registration(user);

        assertThrows(UserAlreadyExistsException.class, () ->
                userService.registration(user));
    }
}