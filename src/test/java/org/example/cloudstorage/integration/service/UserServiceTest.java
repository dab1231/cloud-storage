package org.example.cloudstorage.integration.service;

import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import org.example.cloudstorage.dto.request.UserRequest;
import org.example.cloudstorage.exception.UserAlreadyExistsException;
import org.example.cloudstorage.repository.UserRepository;
import org.example.cloudstorage.service.impl.UserServiceImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserServiceTest {

    final String USERNAME = "test";
    final String PASSWORD = "test";

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserServiceImpl userService;

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    static MinIOContainer minIOContainer = new MinIOContainer("minio/minio:latest");

    @BeforeAll
    static void beforeAll() throws MinioException {
        postgres.start();
        minIOContainer.start();
        MinioClient minioClient =
                (MinioClient.builder()
                        .endpoint(minIOContainer.getS3URL())
                        .credentials(minIOContainer.getUserName(), minIOContainer.getPassword())
                        .build());
        minioClient.makeBucket(MakeBucketArgs.builder().bucket("user-files").build());
    }

    @AfterAll
    static void afterAll() {
        postgres.stop();
        minIOContainer.stop();
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
        dynamicPropertyRegistry.add("minio.url", minIOContainer::getS3URL);
        dynamicPropertyRegistry.add("minio.access-key", minIOContainer::getUserName);
        dynamicPropertyRegistry.add("minio.secret-key", minIOContainer::getPassword);
    }

    @Test
    void registration() throws MinioException {
        var user = new UserRequest(USERNAME, PASSWORD);

        var userResponse = userService.registration(user);

        var actualUser = userRepository.findByUsername(userResponse.username());

        assertTrue(actualUser.isPresent());
        actualUser.ifPresent(actual -> assertEquals(USERNAME, actual.getUsername()));
    }

    @Test
    void throwExceptionIfUserAlreadyExists() throws MinioException {

        var user = new UserRequest(USERNAME, PASSWORD);
        userService.registration(user);

        assertThrows(UserAlreadyExistsException.class, () -> userService.registration(user));
    }
}
