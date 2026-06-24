package org.example.cloudstorage.service;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.messages.DeleteRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cloudstorage.dto.download.DownloadedFile;
import org.example.cloudstorage.dto.response.DirectoryResponse;
import org.example.cloudstorage.dto.response.FileResponse;
import org.example.cloudstorage.dto.response.ResourceResponse;
import org.example.cloudstorage.exception.InvalidBodyException;
import org.example.cloudstorage.exception.InvalidPathException;
import org.example.cloudstorage.exception.ResourceAlreadyExistsException;
import org.example.cloudstorage.exception.ResourceNotFoundException;
import org.example.cloudstorage.security.UserDetailsDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@RequiredArgsConstructor
@Service
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    public ResourceResponse getInfo(String path) throws MinioException {

        ifPathInvalidThrowException(path);

        String fullPath = getUserPrefix() + path;

        if (path.endsWith("/")) {

            var results = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .prefix(fullPath)
                    .build());

            if (!results.iterator().hasNext()) {
                throw new ResourceNotFoundException("Resource not found");
            } else {
                return new DirectoryResponse(
                        getPath(path),
                        getName(path),
                        "DIRECTORY"
                );
            }

        } else {
            StatObjectResponse statObjectResponse = null;
            try {
                statObjectResponse = minioClient.statObject(StatObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fullPath)
                        .build());
            } catch (ErrorResponseException e) {
                if (Objects.equals(e.errorResponse().code(), "NoSuchKey")) {
                    throw new ResourceNotFoundException("Resource not found");
                }
            }

            return new FileResponse(
                    getPath(path),
                    getName(path),
                    statObjectResponse.size(),
                    "FILE"
            );
        }
    }

    public void deleteResource(String path) throws MinioException {

        ifPathInvalidThrowException(path);

        String fullPath = getUserPrefix() + path;

        if (path.endsWith("/")) {

            var results = minioClient.listObjects(ListObjectsArgs.builder()
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

            var deleteResult = minioClient.removeObjects(RemoveObjectsArgs.builder()
                    .bucket(bucketName)
                    .objects(deleteList)
                    .build());

            for (var error : deleteResult) {
                log.warn(error.get().message());
            }

        } else {

            getInfo(path);

            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fullPath)
                    .build());
        }
    }

    public DownloadedFile downloadFile(String path) throws MinioException {

        ifPathInvalidThrowException(path);
        String fullPath = getUserPrefix() + path;

        InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fullPath)
                        .build());
        return new DownloadedFile(getName(path), stream);

    }

    public void downloadDirectory(String path, OutputStream outputStream) throws MinioException, IOException {

        ifPathInvalidThrowException(path);
        String fullPath = getUserPrefix() + path;
        ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);

        var results = minioClient.listObjects(ListObjectsArgs.builder()
                .bucket(bucketName)
                .prefix(fullPath)
                .recursive(true)
                .build());


        for (var itemResult : results) {
            var object = minioClient.getObject(GetObjectArgs.builder()
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
    }

    public ResourceResponse moveResource(String from, String to) throws MinioException {

        ifPathInvalidThrowException(from, to);
        String fullPathTo = getUserPrefix() + to;
        String fullPathFrom = getUserPrefix() + from;

        if (from.endsWith("/")) {
            var results = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .prefix(fullPathTo)
                    .build());

            if (results.iterator().hasNext()) {
                throw new ResourceAlreadyExistsException("Resource with name: " + to + " already exists");
            }

            var objects = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .prefix(fullPathFrom)
                    .recursive(true)
                    .build());

            for (var object : objects) {
                minioClient.copyObject(CopyObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fullPathTo + object.get().objectName().substring(fullPathFrom.length()))
                        .source(SourceObject.builder()
                                .bucket(bucketName)
                                .object(object.get().objectName())
                                .build())
                        .build());
            }

        } else {

            try {
                minioClient.statObject(StatObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fullPathTo)
                        .build());
                throw new ResourceAlreadyExistsException("Resource with name: " + to + " already exists");

            } catch (ErrorResponseException e) {
                if (!Objects.equals(e.errorResponse().code(), "NoSuchKey")) {
                    throw e;
                }
            }

            minioClient.copyObject(CopyObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fullPathTo)
                    .source(SourceObject.builder()
                            .bucket(bucketName)
                            .object(fullPathFrom)
                            .build())
                    .build());

        }
        deleteResource(from);
        return getInfo(to);
    }

    public DirectoryResponse createDirectory(String path) throws MinioException {
        if (path.isBlank() || path.contains("..") || !path.endsWith("/")) {
            throw new InvalidPathException("Directory path must not be blank, must not contain '..', and must end with '/'");
        }
        String fullPath = getUserPrefix() + path;

        var results = minioClient.listObjects(ListObjectsArgs.builder()
                .bucket(bucketName)
                .prefix(fullPath)
                .build());

        if (results.iterator().hasNext()) {
            throw new ResourceAlreadyExistsException("Resource already exists");
        }

        var parentPath = getPath(fullPath);
        if (!parentPath.isEmpty()) {
            var parentResults = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .prefix(parentPath)
                    .build());
            if (!parentResults.iterator().hasNext()) {
                throw new ResourceNotFoundException("Parent directory was not found");
            }
        }

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fullPath).stream(
                                new ByteArrayInputStream(new byte[]{}), 0L, -1L)
                        .build());

        return (DirectoryResponse) getInfo(path);
    }

    public void createUserDirectory(String userDirectoryName) throws MinioException {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(userDirectoryName).stream(
                                new ByteArrayInputStream(new byte[]{}), 0L, -1L)
                        .build());

    }

    public List<ResourceResponse> searchResources(String query) throws MinioException {
        ifPathInvalidThrowException(query);

        var results = minioClient.listObjects(ListObjectsArgs.builder()
                .bucket(bucketName)
                .recursive(true)
                .prefix(getUserPrefix())
                .build());
        List<ResourceResponse> resultList = new ArrayList<>();

        for (var itemResult : results) {
            if (getName(itemResult.get().objectName().toLowerCase(Locale.ROOT)).contains(query.toLowerCase(Locale.ROOT))) {
                var path = itemResult.get().objectName();
                path = path.substring(getUserPrefix().length());

                if (path.endsWith("/")) {
                    resultList.add(
                            new DirectoryResponse(
                                    getPath(path),
                                    getName(path),
                                    "DIRECTORY")
                    );
                } else {
                    resultList.add(
                            new FileResponse(
                                    getPath(path),
                                    getName(path),
                                    itemResult.get().size(),
                                    "FILE")
                    );
                }
            }
        }
        return resultList;
    }

    public List<ResourceResponse> uploadFiles(List<MultipartFile> files, String path) throws MinioException, IOException {

        if (path.contains("..") || (!path.isBlank() && !path.endsWith("/"))) {
            throw new InvalidPathException("Directory path must not be blank, must not contain '..', and must end with '/'");
        }
        String fullPath = getUserPrefix() + path;

        if (files.isEmpty()) {
            throw new InvalidBodyException("Invalid request body");
        }
        List<ResourceResponse> resourceResponses = new ArrayList<>();
        for (MultipartFile file : files) {

            var originalFilename = fullPath.concat(Objects.requireNonNull(file.getOriginalFilename()));
            var responsePath = path.concat(file.getOriginalFilename());
            try {
                minioClient.statObject(StatObjectArgs.builder()
                        .bucket(bucketName)
                        .object(originalFilename)
                        .build());
                throw new ResourceAlreadyExistsException("Resource with name: " + originalFilename + " already exists");

            } catch (ErrorResponseException e) {
                if (!Objects.equals(e.errorResponse().code(), "NoSuchKey")) {
                    throw e;
                }
            }

            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(originalFilename)
                    .stream(file.getInputStream(), file.getSize(), -1L)
                    .contentType(file.getContentType())
                    .build());

            resourceResponses.add(new FileResponse(
                    getPath(responsePath),
                    getName(responsePath),
                    file.getSize(),
                    "FILE"
            ));
        }

        return resourceResponses;
    }

    public List<ResourceResponse> getResourcesInDirectory(String path) throws MinioException {

        if (path.contains("..") || (!path.isBlank() && !path.endsWith("/"))) {
            throw new InvalidPathException("Directory path must not be blank, must not contain '..', and must end with '/'");
        }
        String fullPath = getUserPrefix() + path;

        var results = minioClient.listObjects(ListObjectsArgs.builder()
                .bucket(bucketName)
                .prefix(fullPath)
                .build());

        if (!results.iterator().hasNext() && !path.isEmpty()) {
            throw new ResourceNotFoundException("Resource not found");
        }

        List<ResourceResponse> resultList = new ArrayList<>();

        for (var itemResult : results) {
            if (itemResult.get().objectName().equals(fullPath)) continue;

            var itemPath = itemResult.get().objectName();
            itemPath = itemPath.substring(getUserPrefix().length());

            if (itemPath.endsWith("/")) {
                resultList.add(
                        new DirectoryResponse(
                                getPath(itemPath),
                                getName(itemPath),
                                "DIRECTORY")
                );
            } else {
                resultList.add(
                        new FileResponse(
                                getPath(itemPath),
                                getName(itemPath),
                                itemResult.get().size(),
                                "FILE")
                );
            }
        }

        return resultList;
    }

    private static void ifPathInvalidThrowException(String path) {
        if (path.isBlank() || path.contains("..")) {
            throw new InvalidPathException("Path must not be blank or contain '..'");
        }
    }

    private static void ifPathInvalidThrowException(String from, String to) {
        if (from.isBlank() || from.contains("..") || to.isBlank() || to.contains("..") ||
                from.equals(to) || (from.endsWith("/") && !to.endsWith("/")) ||
                (to.endsWith("/") && !from.endsWith("/"))) {
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

    public String getUserDirectoryName(Long id) {
        return "user-" + id + "-files/";
    }

    private String getUserPrefix() {
        var principal = (UserDetailsDto) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return getUserDirectoryName(principal.getId());
    }
}
