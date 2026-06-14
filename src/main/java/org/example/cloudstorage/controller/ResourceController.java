package org.example.cloudstorage.controller;

import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import org.example.cloudstorage.dto.download.DownloadedFile;
import org.example.cloudstorage.dto.response.DirectoryResponse;
import org.example.cloudstorage.dto.response.ResourceResponse;
import org.example.cloudstorage.service.MinioService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ResourceController {

    private final MinioService minioService;

    @GetMapping("/resource")
    public ResponseEntity<ResourceResponse> getInfo(@RequestParam String path) throws MinioException {

        var info = minioService.getInfo(path);

        return new ResponseEntity<>(info, HttpStatus.OK);
    }

    @DeleteMapping("/resource")
    public ResponseEntity<Object> deleteResource(@RequestParam String path) throws MinioException {

        minioService.deleteResource(path);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/resource/download")
    public ResponseEntity<StreamingResponseBody> downloadResource(@RequestParam String path) throws MinioException {

        if (path.endsWith("/")) {

            var info = (DirectoryResponse) minioService.getInfo(path);
            var directoryName = info.name();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentDisposition(ContentDisposition.attachment().filename(directoryName + ".zip").build());
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            return ResponseEntity.status(HttpStatus.OK)
                    .headers(headers)
                    .body(outputStream -> {
                        try {
                            minioService.downloadDirectory(path, outputStream);
                        } catch (MinioException e) {
                            throw new IOException(e);
                        }
                    });

        } else {

            DownloadedFile downloadedFile = minioService.downloadFile(path);
            var inputStream = downloadedFile.inputStream();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDisposition(ContentDisposition.attachment().filename(downloadedFile.name()).build());

            return ResponseEntity.status(HttpStatus.OK)
                    .headers(headers)
                    .body(outputStream -> {
                                try (inputStream) {
                                    inputStream.transferTo(outputStream);
                                }
                    });

        }
    }

    @GetMapping("/resource/move")
    public ResponseEntity<ResourceResponse> moveResource(@RequestParam String from, @RequestParam String to) throws MinioException {

        var resourceResponse = minioService.moveResource(from, to);
        return ResponseEntity.status(HttpStatus.OK)
                .body(resourceResponse);
    }

    @PostMapping("/directory")
    public ResponseEntity<DirectoryResponse> createDirectory(@RequestParam String path) throws MinioException {

        var directory = minioService.createDirectory(path);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(directory);
    }

    @GetMapping("/resource/search")
    public ResponseEntity<List<ResourceResponse>> searchResources(@RequestParam String query) throws MinioException {

        var resourceResponses = minioService.searchResources(query);

        return ResponseEntity.status(HttpStatus.OK)
                .body(resourceResponses);
    }

    @PostMapping("/resource")
    public ResponseEntity<List<ResourceResponse>> uploadFiles(@RequestParam("object")List<MultipartFile> files, @RequestParam String path) throws MinioException, IOException {

        var fileResponses = minioService.uploadFiles(files, path);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fileResponses);
    }
}
