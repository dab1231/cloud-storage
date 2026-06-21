package org.example.cloudstorage.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @Size(min = 5, max = 20, message = "Value must be between 5 and 20")
        @Pattern(regexp = "^[a-zA-Z0-9]+[a-zA-Z_0-9]*[a-zA-Z0-9]+$")
        @NotBlank(message = "Value must not be blank")
        String username,
        @Size(min = 5, max = 20, message = "Value must be between 5 and 20")
        @Pattern(regexp = "^[a-zA-Z0-9!@#$%^&*(),.?\":{}|<>\\[\\]/`~+=-_';]*$")
        @NotBlank(message = "Value must not be blank")
        String password) {
}
