package org.example.cloudstorage.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Cloud storage app",
        description = "Многопользовательское файловое облако. Пользователи сервиса могут использовать его для загрузки и хранения файлов.",
        version = "1.0"
))
@SecurityScheme(
        name = "session-cookie",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.COOKIE,
        paramName = "SESSION"
)
public class OpenApiConfiguration {
}
