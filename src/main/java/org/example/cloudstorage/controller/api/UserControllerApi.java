package org.example.cloudstorage.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.cloudstorage.dto.response.ErrorResponse;
import org.example.cloudstorage.dto.response.UserResponse;
import org.springframework.http.ResponseEntity;

import java.security.Principal;

@Tag(name = "Get user", description = "Позволяет получить информацию о текущем пользователе")
public interface UserControllerApi {

    @SecurityRequirement(name = "session-cookie")
    @Operation(summary = "Получает username и возвращает его")
    @ApiResponses({
            @ApiResponse(responseCode = "401", description = "Неверные данные", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Неизвестная ошибка", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "200", description = "Успешно", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UserResponse.class)))
    })
    ResponseEntity<UserResponse> getCurrentUser(Principal principal);
}
