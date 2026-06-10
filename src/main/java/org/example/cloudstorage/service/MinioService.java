package org.example.cloudstorage.service;

import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.messages.DeleteRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.cloudstorage.dto.response.DirectoryResponse;
import org.example.cloudstorage.dto.response.FileResponse;
import org.example.cloudstorage.dto.response.ResourceResponse;
import org.example.cloudstorage.exception.InvalidPathException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class MinioService {

    @Value("${minio.bucket-name}")
    private String bucketName;
    private final MinioClient minioClient;

    public ResourceResponse getInfo(String path) throws MinioException {

        ifPathInvalidThrowException(path);

        var statObjectResponse = minioClient.statObject(StatObjectArgs.builder()
                .bucket(bucketName)
                .object(path)
                .build());

        if (path.endsWith("/")) {
            return new DirectoryResponse(
                    getPath(statObjectResponse.object()),
                    getName(path),
                    "DIRECTORY"
            );
        } else {
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

    private static void ifPathInvalidThrowException(String path) {
        if (path.isBlank() || path.contains("..")) {
            throw new InvalidPathException("Path must not be blank or contain '..'");
        }
    }

    private String getName(String path) {
        String name;
        var split = path.split("/");
        if (path.endsWith("/")) {
            name = split[split.length - 2];
        } else {
            name = split[split.length - 1];
        }
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
