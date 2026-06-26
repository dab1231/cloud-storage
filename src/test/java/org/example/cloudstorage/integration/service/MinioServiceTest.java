package org.example.cloudstorage.integration.service;

import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectsArgs;
import io.minio.errors.MinioException;
import io.minio.messages.DeleteRequest;
import org.example.cloudstorage.dto.response.DirectoryResponse;
import org.example.cloudstorage.dto.response.FileResponse;
import org.example.cloudstorage.enums.ResourceType;
import org.example.cloudstorage.enums.Role;
import org.example.cloudstorage.exception.ResourceNotFoundException;
import org.example.cloudstorage.security.UserDetailsDto;
import org.example.cloudstorage.service.MinioService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class MinioServiceTest {

    static final String bucketName = "user-files";

    final UserDetailsDto user1 = new UserDetailsDto(1L, "ivan", "pass", Role.USER);
    final UserDetailsDto user2 = new UserDetailsDto(2L, "sveta", "pass", Role.USER);

    MockMultipartFile multipartFile1 =
            new MockMultipartFile("file", "test1", "text/plain", "test content1".getBytes());
    MockMultipartFile multipartFile2 =
            new MockMultipartFile("file", "test2", "text/plain", "test content2".getBytes());

    @Autowired
    MinioService minioService;
    @Autowired
    MinioClient minioClient;

    static MinIOContainer minIOContainer = new MinIOContainer("minio/minio:latest");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry dynamicPropertyRegistry) {
        dynamicPropertyRegistry.add("minio.url", minIOContainer::getS3URL);
        dynamicPropertyRegistry.add("minio.access-key", minIOContainer::getUserName);
        dynamicPropertyRegistry.add("minio.secret-key", minIOContainer::getPassword);
    }

    @BeforeAll
    static void beforeAll() throws MinioException {
        minIOContainer.start();
        MinioClient minioClient =
                (MinioClient.builder()
                        .endpoint(minIOContainer.getS3URL())
                        .credentials(minIOContainer.getUserName(), minIOContainer.getPassword())
                        .build());
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
    }

    @AfterAll
    static void afterAll() {
        minIOContainer.stop();
    }

    @BeforeEach
    void beforeEach() throws MinioException {
        minioService.createUserDirectory(minioService.getUserDirectoryName(user1.getId()));
    }

    @AfterEach
    void afterEach() throws MinioException {
        var results =
                minioClient.listObjects(
                        ListObjectsArgs.builder().bucket(bucketName).recursive(true).build());

        List<DeleteRequest.Object> deleteList = new ArrayList<>();
        for (var item : results) {
            deleteList.add(new DeleteRequest.Object(item.get().objectName()));
        }

        var removed =
                minioClient.removeObjects(
                        RemoveObjectsArgs.builder().bucket(bucketName).objects(deleteList).build());

        for (var item : removed) {
            // обязательная итерация из за lazy remove object
        }
    }

    @Test
    void uploadFileAppearsInUserRootFolder() throws MinioException, IOException {

        minioService.uploadFiles(List.of(multipartFile1), "", user1.getId());

        var info =
                (FileResponse) minioService.getInfo(multipartFile1.getOriginalFilename(), user1.getId());
        assertThat(info.path()).isEqualTo("");
        assertThat(info.name()).isEqualTo("test1");
        assertThat(info.type()).isEqualTo(ResourceType.FILE);
        assertThat(info.size()).isEqualTo(multipartFile1.getSize());
    }

    @Test
    void renameFile() throws MinioException, IOException {

        minioService.uploadFiles(List.of(multipartFile1), "", user1.getId());

        minioService.moveResource(multipartFile1.getOriginalFilename(), "testname", user1.getId());
        var info = (FileResponse) minioService.getInfo("testname", user1.getId());
        assertThat(info.name()).isEqualTo("testname");

        Assertions.assertThrows(
                ResourceNotFoundException.class,
                () -> minioService.getInfo(multipartFile1.getOriginalFilename(), user1.getId()));
    }

    @Test
    void deleteFile() throws MinioException, IOException {

        minioService.uploadFiles(List.of(multipartFile1), "", user1.getId());
        minioService.deleteResource(multipartFile1.getOriginalFilename(), user1.getId());

        Assertions.assertThrows(
                ResourceNotFoundException.class,
                () -> minioService.getInfo(multipartFile1.getOriginalFilename(), user1.getId()));
    }

    @Test
    void renameDirectory() throws MinioException, IOException {

        minioService.uploadFiles(List.of(multipartFile1, multipartFile2), "testfolder/", user1.getId());

        minioService.moveResource("testfolder/", "folder/", user1.getId());
        var info = (DirectoryResponse) minioService.getInfo("folder/", user1.getId());
        var resourcesInDirectory = minioService.getResourcesInDirectory("folder/", user1.getId());

        assertThat(info.name()).isEqualTo("folder/");
        assertThat(info.path()).isEqualTo("");

        var file1 = (FileResponse) resourcesInDirectory.get(0);
        var file2 = (FileResponse) resourcesInDirectory.get(1);
        assertThat(resourcesInDirectory.size()).isEqualTo(2);
        assertThat(file1.name()).isEqualTo(multipartFile1.getOriginalFilename());
        assertThat(file2.name()).isEqualTo(multipartFile2.getOriginalFilename());

        Assertions.assertThrows(
                ResourceNotFoundException.class, () -> minioService.getInfo("testfolder/", user1.getId()));
    }

    @Test
    void deleteDirectory() throws MinioException, IOException {
        minioService.uploadFiles(List.of(multipartFile1, multipartFile2), "testfolder/", user1.getId());

        minioService.deleteResource("testfolder/", user1.getId());
        Assertions.assertThrows(
                ResourceNotFoundException.class, () -> minioService.getInfo("testfolder/", user1.getId()));
    }

    @Nested
    class CrossUserIsolationTests {

        @BeforeEach
        void beforeEach() throws MinioException {
            minioService.createUserDirectory(minioService.getUserDirectoryName(user2.getId()));
        }

        @Test
        void userCannotAccessAnotherUsersFile() throws MinioException, IOException {
            minioService.uploadFiles(List.of(multipartFile1), "", user1.getId());

            Assertions.assertThrows(
                    ResourceNotFoundException.class,
                    () -> minioService.getInfo(multipartFile1.getOriginalFilename(), user2.getId()));
        }

        @Test
        void searchReturnsOwnFile() throws MinioException, IOException {
            minioService.uploadFiles(List.of(multipartFile2), "", user2.getId());
            minioService.uploadFiles(List.of(multipartFile1), "", user1.getId());

            var user1Resources = minioService.searchResources("test", user1.getId());
            assertThat(user1Resources.size()).isEqualTo(1);
            var user1FileResponse = (FileResponse) user1Resources.getFirst();
            assertThat(user1FileResponse.name()).isEqualTo("test1");

            var user2Resources = minioService.searchResources("test", user2.getId());
            assertThat(user2Resources.size()).isEqualTo(1);
            var user2FileResponse = (FileResponse) user2Resources.getFirst();
            assertThat(user2FileResponse.name()).isEqualTo("test2");
        }
    }
}
