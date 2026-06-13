package org.example.cloudstorage.controller;

import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import org.example.cloudstorage.dto.download.DownloadedFile;
import org.example.cloudstorage.dto.response.DirectoryResponse;
import org.example.cloudstorage.dto.response.ResourceResponse;
import org.example.cloudstorage.service.MinioService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;

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
    public ResponseEntity<StreamingResponseBody> downloadResource(@RequestParam String path) throws MinioException, IOException {

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
}
