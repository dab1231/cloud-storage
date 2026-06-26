package org.example.cloudstorage.service;

import io.minio.errors.MinioException;
import org.example.cloudstorage.dto.download.DownloadedResource;
import org.example.cloudstorage.dto.response.DirectoryResponse;
import org.example.cloudstorage.dto.response.ResourceResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface MinioService {

    ResourceResponse getInfo(String path) throws MinioException;

    void deleteResource(String path) throws MinioException;

    DownloadedResource downloadedResource(String path) throws MinioException, IOException;

    ResourceResponse moveResource(String from, String to) throws MinioException;

    DirectoryResponse createDirectory(String path) throws MinioException;

    void createUserDirectory(String userDirectoryName) throws MinioException;

    List<ResourceResponse> searchResources(String query) throws MinioException;

    List<ResourceResponse> uploadFiles(List<MultipartFile> files, String path)
            throws MinioException, IOException;

    List<ResourceResponse> getResourcesInDirectory(String path) throws MinioException;

    String getUserDirectoryName(Long id);
}
