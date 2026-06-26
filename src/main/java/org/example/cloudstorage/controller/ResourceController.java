package org.example.cloudstorage.controller;

import io.minio.errors.MinioException;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.cloudstorage.controller.api.ResourceControllerApi;
import org.example.cloudstorage.dto.response.DirectoryResponse;
import org.example.cloudstorage.dto.response.ResourceResponse;
import org.example.cloudstorage.service.MinioService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequiredArgsConstructor
public class ResourceController implements ResourceControllerApi {

  private final MinioService minioService;

  @Override
  public ResponseEntity<ResourceResponse> getInfo(String path) throws MinioException {

    var info = minioService.getInfo(path);

    return new ResponseEntity<>(info, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Object> deleteResource(String path) throws MinioException {

    minioService.deleteResource(path);

    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<StreamingResponseBody> downloadResource(String path)
      throws MinioException, IOException {

    var downloadedResource = minioService.downloadedResource(path);

    try (var inputStream = downloadedResource.inputStream()) {
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
  }

  @Override
  public ResponseEntity<ResourceResponse> moveResource(String from, String to)
      throws MinioException {

    var resourceResponse = minioService.moveResource(from, to);
    return ResponseEntity.status(HttpStatus.OK).body(resourceResponse);
  }

  @Override
  public ResponseEntity<DirectoryResponse> createDirectory(String path) throws MinioException {

    var directory = minioService.createDirectory(path);

    return ResponseEntity.status(HttpStatus.CREATED).body(directory);
  }

  @Override
  public ResponseEntity<List<ResourceResponse>> searchResources(String query)
      throws MinioException {

    var resourceResponses = minioService.searchResources(query);

    return ResponseEntity.status(HttpStatus.OK).body(resourceResponses);
  }

  @Override
  public ResponseEntity<List<ResourceResponse>> uploadFiles(List<MultipartFile> files, String path)
      throws MinioException, IOException {

    var fileResponses = minioService.uploadFiles(files, path);

    return ResponseEntity.status(HttpStatus.CREATED).body(fileResponses);
  }

  @Override
  public ResponseEntity<List<ResourceResponse>> getResourcesInDirectory(String path)
      throws MinioException {

    var resourcesInDirectory = minioService.getResourcesInDirectory(path);

    return ResponseEntity.status(HttpStatus.OK).body(resourcesInDirectory);
  }
}
