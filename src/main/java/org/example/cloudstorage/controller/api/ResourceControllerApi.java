package org.example.cloudstorage.controller.api;

import io.minio.errors.MinioException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.cloudstorage.dto.response.DirectoryResponse;
import org.example.cloudstorage.dto.response.ErrorResponse;
import org.example.cloudstorage.dto.response.FileResponse;
import org.example.cloudstorage.dto.response.ResourceResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.util.List;

@Tag(name = "Resource controller", description = "Позволяет производить манипуляции над ресурсами")
public interface ResourceControllerApi {

    @SecurityRequirement(name = "session-cookie")
    @Operation(summary = "Получить информацию о ресурсе")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Невалидный или отсутствующий путь",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ресурс не найден",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Неизвестная ошибка",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "200", description = "Успешно",
                    content = @Content(mediaType = "application/json", schema = @Schema(oneOf = {FileResponse.class, DirectoryResponse.class}, type = "object")))
    })
    ResponseEntity<ResourceResponse> getInfo(
            @Parameter(description = "путь к ресурсу",
                    required = true) String path
    ) throws MinioException;

    @SecurityRequirement(name = "session-cookie")
    @Operation(summary = "Удалить ресурс")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Невалидный или отсутствующий путь",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ресурс не найден",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Неизвестная ошибка",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "204", description = "Успешно")
    })
    ResponseEntity<Object> deleteResource(
            @Parameter(description = "путь к ресурсу для удаления",
                    required = true) String path
    ) throws MinioException;


    @SecurityRequirement(name = "session-cookie")
    @Operation(summary = "Переместить или переименовать ресурс")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Невалидный или отсутствующий путь",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ресурс не найден",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ресурс по пути to уже существует",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Неизвестная ошибка",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "200", description = "Успешно",
                    content = @Content(mediaType = "application/json", schema = @Schema(oneOf = {FileResponse.class, DirectoryResponse.class}, type = "object")))
    })
    ResponseEntity<ResourceResponse> moveResource(
            @Parameter(description = "откуда переместить/переименовать", required = true)
            String from,
            @Parameter(description = "куда переместить/переименовать", required = true)
            String to
    ) throws MinioException;


    @SecurityRequirement(name = "session-cookie")
    @Operation(summary = "Создать папку")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Невалидный или отсутствующий путь к новой папке",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Родительская папка не существует",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Папка уже существует",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Неизвестная ошибка",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "201", description = "Успешно",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = DirectoryResponse.class)))
    })
    ResponseEntity<DirectoryResponse> createDirectory(
            @Parameter(description = "путь к новой папке",
                    required = true) String path
    ) throws MinioException;

    @SecurityRequirement(name = "session-cookie")
    @Operation(summary = "Найти ресурсы по названию")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Невалидный или отсутствующий поисковой запрос",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Неизвестная ошибка",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "200", description = "Успешно",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(oneOf = {FileResponse.class, DirectoryResponse.class}, type = "object"))))
    })
    ResponseEntity<List<ResourceResponse>> searchResources(
            @Parameter(description = "поисковой запрос",
                    required = true) String query
    ) throws MinioException;

    @SecurityRequirement(name = "session-cookie")
    @Operation(summary = "Получение информации о содержимом папки")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Невалидный или отсутствующий путь",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Папка не существует",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Неизвестная ошибка",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "200", description = "Успешно",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(oneOf = {FileResponse.class, DirectoryResponse.class}, type = "object"))))
    })
    ResponseEntity<List<ResourceResponse>> getResourcesInDirectory(
            @Parameter(description = "путь к папке",
                    required = true) String path
    ) throws MinioException;

    @SecurityRequirement(name = "session-cookie")
    @Operation(summary = "Скачать ресурс")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Невалидный или отсутствующий путь",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Ресурс не найден",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Неизвестная ошибка",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "200", description = "Успешно",
                    content = @Content(mediaType = "application/octet-stream", schema = @Schema(type = "string", format = "binary")))
    })
    ResponseEntity<StreamingResponseBody> downloadResource(
            @Parameter(description = "путь к ресурсу для скачивания",
                    required = true) String path
    ) throws MinioException;


    @SecurityRequirement(name = "session-cookie")
    @Operation(summary = "Загрузка файла/ов в папку")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Невалидное тело запроса",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Файл уже существует",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Неизвестная ошибка",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "201", description = "Успешно",
                    content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = FileResponse.class))))
    })
    ResponseEntity<List<ResourceResponse>> uploadFiles(
            @RequestParam("object") List<MultipartFile> files,
            @Parameter(description = "путь к папке в которую загружаем ресурс(ы)", required = true) String path
    ) throws MinioException, IOException;
}
