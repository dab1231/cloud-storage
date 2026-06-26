package org.example.cloudstorage.controller;

import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import org.example.cloudstorage.controller.api.ResourceControllerApi;
import org.example.cloudstorage.dto.response.DirectoryResponse;
import org.example.cloudstorage.dto.response.ResourceResponse;
import org.example.cloudstorage.security.UserDetailsDto;
import org.example.cloudstorage.service.MinioService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ResourceController implements ResourceControllerApi {

    private final MinioService minioService;

    @Override
    public ResponseEntity<ResourceResponse> getInfo(String path, Principal principal)
            throws MinioException {

        UserDetailsDto userDetails = (UserDetailsDto) principal;
        var info = minioService.getInfo(path, userDetails.getId());

        return new ResponseEntity<>(info, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Object> deleteResource(String path, Principal principal)
            throws MinioException {

        UserDetailsDto userDetails = (UserDetailsDto) principal;
        minioService.deleteResource(path, userDetails.getId());

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public ResponseEntity<StreamingResponseBody> downloadResource(String path, Principal principal)
            throws MinioException, IOException {

        UserDetailsDto userDetails = (UserDetailsDto) principal;
        var downloadedResource = minioService.downloadedResource(path, userDetails.getId());

        var inputStream = downloadedResource.inputStream();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(downloadedResource.name()).build());
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.status(HttpStatus.OK)
                .headers(headers)
                .body(
                        outputStream -> {
                            try (inputStream) {
                                inputStream.transferTo(outputStream);
                            }
                        });
    }

    @Override
    public ResponseEntity<ResourceResponse> moveResource(String from, String to, Principal principal)
            throws MinioException {

        UserDetailsDto userDetails = (UserDetailsDto) principal;
        var resourceResponse = minioService.moveResource(from, to, userDetails.getId());
        return ResponseEntity.status(HttpStatus.OK).body(resourceResponse);
    }

    @Override
    public ResponseEntity<DirectoryResponse> createDirectory(String path, Principal principal)
            throws MinioException {

        UserDetailsDto userDetails = (UserDetailsDto) principal;
        var directory = minioService.createDirectory(path, userDetails.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(directory);
    }

    @Override
    public ResponseEntity<List<ResourceResponse>> searchResources(String query, Principal principal)
            throws MinioException {

        UserDetailsDto userDetails = (UserDetailsDto) principal;
        var resourceResponses = minioService.searchResources(query, userDetails.getId());

        return ResponseEntity.status(HttpStatus.OK).body(resourceResponses);
    }

    @Override
    public ResponseEntity<List<ResourceResponse>> uploadFiles(
            List<MultipartFile> files, String path, Principal principal)
            throws MinioException, IOException {

        UserDetailsDto userDetails = (UserDetailsDto) principal;
        var fileResponses = minioService.uploadFiles(files, path, userDetails.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(fileResponses);
    }

    @Override
    public ResponseEntity<List<ResourceResponse>> getResourcesInDirectory(
            String path, Principal principal) throws MinioException {

        UserDetailsDto userDetails = (UserDetailsDto) principal;
        var resourcesInDirectory = minioService.getResourcesInDirectory(path, userDetails.getId());

        return ResponseEntity.status(HttpStatus.OK).body(resourcesInDirectory);
    }
}
