package org.example.cloudstorage.service;

import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.messages.DeleteRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cloudstorage.dto.download.DownloadedFile;
import org.example.cloudstorage.dto.response.DirectoryResponse;
import org.example.cloudstorage.dto.response.FileResponse;
import org.example.cloudstorage.dto.response.ResourceResponse;
import org.example.cloudstorage.exception.InvalidPathException;
import org.example.cloudstorage.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
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

        if (path.endsWith("/")) {

            var results = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .prefix(path)
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
            var statObjectResponse = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(path)
                    .build());

            return new FileResponse(
                    getPath(statObjectResponse.object()),
                    getName(path),
                    statObjectResponse.size(),
                    "FILE"
            );
        }
    }

    public void deleteResource(String path) throws MinioException {

        ifPathInvalidThrowException(path);

        if (path.endsWith("/")) {

            var results = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .prefix(path)
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
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(path)
                    .build());
        }
    }

    public DownloadedFile downloadFile(String path) throws MinioException {

        ifPathInvalidThrowException(path);

        InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(path)
                        .build());
        return new DownloadedFile(getName(path), stream);

    }

    public void downloadDirectory(String path, OutputStream outputStream) throws MinioException, IOException {

        ifPathInvalidThrowException(path);

        ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);

        var results = minioClient.listObjects(ListObjectsArgs.builder()
                .bucket(bucketName)
                .prefix(path)
                .recursive(true)
                .build());


        for (var itemResult : results) {
            var object = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(itemResult.get().objectName())
                    .build());

            String name = object.object().substring(path.length());

            zipOutputStream.putNextEntry(new ZipEntry(name));
            object.transferTo(zipOutputStream);
            object.close();
            zipOutputStream.closeEntry();
        }
        zipOutputStream.close();
    }

    private static void ifPathInvalidThrowException(String path) {
        if (path.isBlank() || path.contains("..")) {
            throw new InvalidPathException("Path must not be blank or contain '..'");
        }
    }

    private String getName(String path) {
        String name;
        var split = path.split("/");
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
}
