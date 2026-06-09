package org.example.cloudstorage.dto.response;

public record DirectoryResponse(String path, String name, String type) implements MinioRespImpl{
}
