package org.example.cloudstorage.service;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class MinioService {

    private final MinioClient minioClient;
}
