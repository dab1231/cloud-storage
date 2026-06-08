package org.example.cloudstorage.dto.response;

import java.util.List;

public record ErrorResponse(
        List<String> message) {
}
