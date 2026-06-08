package org.example.cloudstorage.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UserRequest(
        @NotBlank String username,
        @NotBlank String password) {
}
