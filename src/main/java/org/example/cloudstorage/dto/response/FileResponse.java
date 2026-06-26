package org.example.cloudstorage.dto.response;

import org.example.cloudstorage.enums.ResourceType;

public record FileResponse(String path, String name, Long size, ResourceType type)
        implements ResourceResponse {
}
