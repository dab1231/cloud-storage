package org.example.cloudstorage.service.impl;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.messages.DeleteRequest;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cloudstorage.dto.download.DownloadedResource;
import org.example.cloudstorage.dto.response.DirectoryResponse;
import org.example.cloudstorage.dto.response.FileResponse;
import org.example.cloudstorage.dto.response.ResourceResponse;
import org.example.cloudstorage.enums.ResourceType;
import org.example.cloudstorage.exception.InvalidBodyException;
import org.example.cloudstorage.exception.InvalidPathException;
import org.example.cloudstorage.exception.ResourceAlreadyExistsException;
import org.example.cloudstorage.exception.ResourceNotFoundException;
import org.example.cloudstorage.service.MinioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@RequiredArgsConstructor
@Service
public class MinioServiceImpl implements MinioService {

    public static final long OBJECT_SIZE = 0L;
    public static final long PART_SIZE = -1L;
    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @PostConstruct
    private void createBucketIfNotExist() throws MinioException {
        var exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    @Override
    public ResourceResponse getInfo(String path, Long id) throws MinioException {

        ifPathInvalidThrowException(path);

        String fullPath = getUserDirectoryName(id) + path;

        if (path.endsWith("/")) {

            var results =
                    minioClient.listObjects(
                            ListObjectsArgs.builder().bucket(bucketName).prefix(fullPath).build());

            if (!results.iterator().hasNext()) {
                throw new ResourceNotFoundException("Resource not found");
            } else {
                return new DirectoryResponse(getPath(path), getName(path), ResourceType.DIRECTORY);
            }

        } else {
            StatObjectResponse statObjectResponse = null;
            try {
                statObjectResponse =
                        minioClient.statObject(
                                StatObjectArgs.builder().bucket(bucketName).object(fullPath).build());
            } catch (ErrorResponseException e) {
                if (Objects.equals(e.errorResponse().code(), "NoSuchKey")) {
                    throw new ResourceNotFoundException("Resource not found");
                }
            }

            assert statObjectResponse != null;
            return new FileResponse(
                    getPath(path), getName(path), statObjectResponse.size(), ResourceType.FILE);
        }
    }

    @Override
    public void deleteResource(String path, Long id) throws MinioException {

        ifPathInvalidThrowException(path);

        String fullPath = getUserDirectoryName(id) + path;

        if (path.endsWith("/")) {

            var results =
                    minioClient.listObjects(
                            ListObjectsArgs.builder()
                                    .bucket(bucketName)
                                    .prefix(fullPath)
                                    .recursive(true)
                                    .build());

            List<DeleteRequest.Object> deleteList = new ArrayList<>();
            for (var itemResult : results) {
                deleteList.add(new DeleteRequest.Object(itemResult.get().objectName()));
            }

            if (deleteList.isEmpty()) {
                throw new ResourceNotFoundException("Resource not found");
            }

            var deleteResult =
                    minioClient.removeObjects(
                            RemoveObjectsArgs.builder().bucket(bucketName).objects(deleteList).build());

            for (var error : deleteResult) {
                log.warn(error.get().message());
            }

        } else {

            getInfo(path, id);

            minioClient.removeObject(
                    RemoveObjectArgs.builder().bucket(bucketName).object(fullPath).build());
        }
    }

    @Override
    public DownloadedResource downloadedResource(String path, Long id)
            throws MinioException, IOException {
        if (path.endsWith("/")) {
            return downloadDirectory(path, id);
        } else {
            return downloadFile(path, id);
        }
    }

    private DownloadedResource downloadFile(String path, Long id) throws MinioException {

        ifPathInvalidThrowException(path);
        String fullPath = getUserDirectoryName(id) + path;

        InputStream stream =
                minioClient.getObject(GetObjectArgs.builder().bucket(bucketName).object(fullPath).build());
        return new DownloadedResource(getName(path), stream);
    }

    private DownloadedResource downloadDirectory(String path, Long id)
            throws MinioException, IOException {

        ifPathInvalidThrowException(path);
        String fullPath = getUserDirectoryName(id) + path;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream);

        var results =
                minioClient.listObjects(
                        ListObjectsArgs.builder().bucket(bucketName).prefix(fullPath).recursive(true).build());

        for (var itemResult : results) {
            var object =
                    minioClient.getObject(
                            GetObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(itemResult.get().objectName())
                                    .build());

            String name = object.object().substring(fullPath.length());

            zipOutputStream.putNextEntry(new ZipEntry(name));
            object.transferTo(zipOutputStream);
            object.close();
            zipOutputStream.closeEntry();
        }
        zipOutputStream.close();
        var byteArray = byteArrayOutputStream.toByteArray();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        var name = getName(path);
        name = name.substring(0, name.length() - 1);
        return new DownloadedResource(name + ".zip", byteArrayInputStream);
    }

    @Override
    public ResourceResponse moveResource(String from, String to, Long id) throws MinioException {

        ifPathInvalidThrowException(from, to);
        String fullPathTo = getUserDirectoryName(id) + to;
        String fullPathFrom = getUserDirectoryName(id) + from;

        if (from.endsWith("/")) {
            var results =
                    minioClient.listObjects(
                            ListObjectsArgs.builder().bucket(bucketName).prefix(fullPathTo).build());

            if (results.iterator().hasNext()) {
                throw new ResourceAlreadyExistsException("Resource with name: " + to + " already exists");
            }

            var objects =
                    minioClient.listObjects(
                            ListObjectsArgs.builder()
                                    .bucket(bucketName)
                                    .prefix(fullPathFrom)
                                    .recursive(true)
                                    .build());

            for (var object : objects) {
                minioClient.copyObject(
                        CopyObjectArgs.builder()
                                .bucket(bucketName)
                                .object(fullPathTo + object.get().objectName().substring(fullPathFrom.length()))
                                .source(
                                        SourceObject.builder()
                                                .bucket(bucketName)
                                                .object(object.get().objectName())
                                                .build())
                                .build());
            }

        } else {

            try {
                minioClient.statObject(
                        StatObjectArgs.builder().bucket(bucketName).object(fullPathTo).build());
                throw new ResourceAlreadyExistsException("Resource with name: " + to + " already exists");

            } catch (ErrorResponseException e) {
                if (!Objects.equals(e.errorResponse().code(), "NoSuchKey")) {
                    throw e;
                }
            }

            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fullPathTo)
                            .source(SourceObject.builder().bucket(bucketName).object(fullPathFrom).build())
                            .build());
        }
        deleteResource(from, id);
        return getInfo(to, id);
    }

    @Override
    public DirectoryResponse createDirectory(String path, Long id) throws MinioException {
        if (path.isBlank() || path.contains("..") || !path.endsWith("/")) {
            throw new InvalidPathException(
                    "Directory path must not be blank, must not contain '..', and must end with '/'");
        }
        String fullPath = getUserDirectoryName(id) + path;

        var results =
                minioClient.listObjects(
                        ListObjectsArgs.builder().bucket(bucketName).prefix(fullPath).build());

        if (results.iterator().hasNext()) {
            throw new ResourceAlreadyExistsException("Resource already exists");
        }

        var parentPath = getPath(fullPath);
        if (!parentPath.isEmpty()) {
            var parentResults =
                    minioClient.listObjects(
                            ListObjectsArgs.builder().bucket(bucketName).prefix(parentPath).build());
            if (!parentResults.iterator().hasNext()) {
                throw new ResourceNotFoundException("Parent directory was not found");
            }
        }

        minioClient.putObject(
                PutObjectArgs.builder().bucket(bucketName).object(fullPath).stream(
                                new ByteArrayInputStream(new byte[]{}), OBJECT_SIZE, PART_SIZE)
                        .build());

        return (DirectoryResponse) getInfo(path, id);
    }

    @Override
    public void createUserDirectory(String userDirectoryName) throws MinioException {
        minioClient.putObject(
                PutObjectArgs.builder().bucket(bucketName).object(userDirectoryName).stream(
                                new ByteArrayInputStream(new byte[]{}), OBJECT_SIZE, PART_SIZE)
                        .build());
    }

    @Override
    public List<ResourceResponse> searchResources(String query, Long id) throws MinioException {
        ifPathInvalidThrowException(query);

        var results =
                minioClient.listObjects(
                        ListObjectsArgs.builder()
                                .bucket(bucketName)
                                .recursive(true)
                                .prefix(getUserDirectoryName(id))
                                .build());
        List<ResourceResponse> resultList = new ArrayList<>();

        for (var itemResult : results) {
            if (getName(itemResult.get().objectName().toLowerCase(Locale.ROOT))
                    .contains(query.toLowerCase(Locale.ROOT))) {
                addResourceToList(resultList, itemResult, id);
            }
        }
        return resultList;
    }

    @Override
    public List<ResourceResponse> uploadFiles(List<MultipartFile> files, String path, Long id)
            throws MinioException, IOException {

        if (path.contains("..") || (!path.isBlank() && !path.endsWith("/"))) {
            throw new InvalidPathException(
                    "Directory path must not be blank, must not contain '..', and must end with '/'");
        }
        String fullPath = getUserDirectoryName(id) + path;

        if (files.isEmpty()) {
            throw new InvalidBodyException("Invalid request body");
        }
        List<ResourceResponse> resourceResponses = new ArrayList<>();
        for (MultipartFile file : files) {

            var originalFilename = fullPath.concat(Objects.requireNonNull(file.getOriginalFilename()));
            var responsePath = path.concat(file.getOriginalFilename());
            try {
                minioClient.statObject(
                        StatObjectArgs.builder().bucket(bucketName).object(originalFilename).build());
                throw new ResourceAlreadyExistsException(
                        "Resource with name: " + originalFilename + " already exists");

            } catch (ErrorResponseException e) {
                if (!Objects.equals(e.errorResponse().code(), "NoSuchKey")) {
                    throw e;
                }
            }

            minioClient.putObject(
                    PutObjectArgs.builder().bucket(bucketName).object(originalFilename).stream(
                                    file.getInputStream(), file.getSize(), PART_SIZE)
                            .contentType(file.getContentType())
                            .build());

            resourceResponses.add(
                    new FileResponse(
                            getPath(responsePath), getName(responsePath), file.getSize(), ResourceType.FILE));
        }

        return resourceResponses;
    }

    @Override
    public List<ResourceResponse> getResourcesInDirectory(String path, Long id)
            throws MinioException {

        if (path.contains("..") || (!path.isBlank() && !path.endsWith("/"))) {
            throw new InvalidPathException(
                    "Directory path must not be blank, must not contain '..', and must end with '/'");
        }
        String fullPath = getUserDirectoryName(id) + path;

        var results =
                minioClient.listObjects(
                        ListObjectsArgs.builder().bucket(bucketName).prefix(fullPath).build());

        if (!results.iterator().hasNext() && !path.isEmpty()) {
            throw new ResourceNotFoundException("Resource not found");
        }

        List<ResourceResponse> resultList = new ArrayList<>();

        for (var itemResult : results) {
            if (itemResult.get().objectName().equals(fullPath)) continue;

            addResourceToList(resultList, itemResult, id);
        }

        return resultList;
    }

    private void addResourceToList(
            List<ResourceResponse> resultList, Result<Item> itemResult, Long id) throws MinioException {
        var itemPath = itemResult.get().objectName();
        itemPath = itemPath.substring(getUserDirectoryName(id).length());

        if (itemPath.endsWith("/")) {
            resultList.add(
                    new DirectoryResponse(getPath(itemPath), getName(itemPath), ResourceType.DIRECTORY));
        } else {
            resultList.add(
                    new FileResponse(
                            getPath(itemPath), getName(itemPath), itemResult.get().size(), ResourceType.FILE));
        }
    }

    private static void ifPathInvalidThrowException(String path) {
        if (path.isBlank() || path.contains("..")) {
            throw new InvalidPathException("Path must not be blank or contain '..'");
        }
    }

    private static void ifPathInvalidThrowException(String from, String to) {
        if (from.isBlank()
                || from.contains("..")
                || to.isBlank()
                || to.contains("..")
                || from.equals(to)
                || (from.endsWith("/") && !to.endsWith("/"))
                || (to.endsWith("/") && !from.endsWith("/"))) {
            throw new InvalidPathException("Invalid path to or from");
        }
    }

    private String getName(String path) {
        String name;
        var split = path.split("/");
        if (path.endsWith("/")) {
            name = split[split.length - 1];
            return name + "/";
        }
        name = split[split.length - 1];
        return name;
    }

    private String getPath(String path) {

        if (path.lastIndexOf('/') == path.length() - 1) {

            var substring = path.substring(0, path.length() - 1);
            var index = substring.lastIndexOf('/');
            return substring.substring(0, index + 1);
        } else {
            var index = path.lastIndexOf('/');
            return path.substring(0, index + 1);
        }
    }

    @Override
    public String getUserDirectoryName(Long id) {
        return "user-" + id + "-files/";
    }
}
