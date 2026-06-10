package org.example.cloudstorage.controller;

import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import org.example.cloudstorage.dto.response.ResourceResponse;
import org.example.cloudstorage.service.MinioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
