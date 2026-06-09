package org.example.cloudstorage.dto.response;

public record FileResponse(String path, String name, Long size, String type) implements MinioRespImpl{
}
