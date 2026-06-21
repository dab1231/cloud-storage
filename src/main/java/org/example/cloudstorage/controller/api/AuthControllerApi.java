package org.example.cloudstorage.controller.api;

import io.minio.errors.MinioException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.example.cloudstorage.dto.request.UserRequest;
import org.example.cloudstorage.dto.response.ErrorResponse;
import org.example.cloudstorage.dto.response.UserResponse;
import org.springframework.http.ResponseEntity;

@Tag(name = "Auth", description = "Обрабатывает sign-in и sign-up запросы")
public interface AuthControllerApi {

    @Operation(summary = "Зарегистрировать пользователя и добавить его в бд")
    @ApiResponses({
            @ApiResponse(responseCode = "500", description = "Неизвестная ошибка", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Username занят", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "201", description = "Пользователь создан", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UserResponse.class))),
    })
    ResponseEntity<UserResponse> registration(@RequestBody(description = "Данные пользователя из формы",
                                                      content = @Content(mediaType = "application/json",
                                                              schema = @Schema(implementation = UserRequest.class)))
                                              UserRequest userRequest,
                                              HttpServletRequest request) throws MinioException;

    @Operation(summary = "Логин пользователя")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Неверные данные", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Неизвестная ошибка", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "200", description = "Успешно", content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UserResponse.class)))
    })
    ResponseEntity<UserResponse> login(@RequestBody(description = "Данные пользователя с формы",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserRequest.class)))
                                       UserRequest userRequest, HttpServletRequest request);

}
