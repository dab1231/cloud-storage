package org.example.cloudstorage.dto.response;

import org.example.cloudstorage.enums.ResourceType;

public record DirectoryResponse(String path, String name, ResourceType type)
        implements ResourceResponse {
}
