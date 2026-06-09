package org.example.cloudstorage.service;

import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import org.example.cloudstorage.dto.response.DirectoryResponse;
import org.example.cloudstorage.dto.response.FileResponse;
import org.example.cloudstorage.dto.response.MinioRespImpl;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class MinioService {

    private final MinioClient minioClient;

    public MinioRespImpl getInfo(String path) throws MinioException {

        var statObjectResponse = minioClient.statObject(StatObjectArgs.builder()
                .bucket("user-files")
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
