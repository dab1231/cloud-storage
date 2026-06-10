package org.example.cloudstorage.controller;

import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import org.example.cloudstorage.dto.response.ResourceResponse;
import org.example.cloudstorage.service.MinioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
