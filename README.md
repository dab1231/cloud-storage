# Cloud Storage

Многопользовательское файловое облако (упрощённый клон Google Drive). Пользователи регистрируются, загружают файлы и
папки, скачивают их (в том числе архивом), перемещают, удаляют и ищут по имени.

Учебный проект — пишется как практика Java/Spring Boot.

## Стек

- **Backend:** Spring Boot 3.4.5, Java 21
- **Auth:** Spring Security + сессии в Redis (Spring Session)
- **БД:** PostgreSQL + Spring Data JPA + Liquibase
- **Файловое хранилище:** MinIO (S3-совместимое), бакет `user-files`, у каждого пользователя своя папка
  `user-${id}-files/`
- **Frontend:** готовый React SPA (собранный бандл лежит в `src/main/resources/static`)
- **Тесты:** JUnit 5 + Testcontainers (Postgres, MinIO)
- **API-документация:** springdoc-openapi (Swagger UI)

## Возможности

- Регистрация / логин / логаут (сессии хранятся в Redis)
- Создание папок, загрузка файлов (в том числе пачкой)
- Просмотр содержимого папки, информация о ресурсе
- Скачивание файла или папки (папка отдаётся zip-архивом)
- Перемещение / переименование ресурсов
- Удаление файлов и папок
- Поиск ресурсов по имени

## API

Все эндпоинты под `/api`. Полная интерактивная документация — Swagger UI:

```
http://localhost:8080/swagger-ui.html
```

Краткая сводка:

| Метод  | Путь                           | Описание                           |
|--------|--------------------------------|------------------------------------|
| POST   | `/api/auth/sign-up`            | Регистрация                        |
| POST   | `/api/auth/sign-in`            | Вход                               |
| POST   | `/api/auth/sign-out`           | Выход                              |
| GET    | `/api/user/me`                 | Текущий пользователь               |
| GET    | `/api/resource?path=`          | Информация о ресурсе               |
| DELETE | `/api/resource?path=`          | Удаление ресурса                   |
| GET    | `/api/resource/download?path=` | Скачать файл / zip-архив папки     |
| GET    | `/api/resource/move?from=&to=` | Переместить / переименовать ресурс |
| GET    | `/api/resource/search?query=`  | Поиск ресурсов                     |
| POST   | `/api/resource?path=`          | Загрузка файлов (multipart)        |
| GET    | `/api/directory?path=`         | Список ресурсов в папке            |
| POST   | `/api/directory?path=`         | Создание папки                     |

Формат ошибок единый для всех эндпоинтов: `{"message": "Текст ошибки"}`.

## Запуск локально

Понадобится Java 21 и Docker.

1. Поднять инфраструктуру (Postgres, MinIO, Redis, Adminer):
   ```
   docker compose up -d
   ```
   Перед этим создать в корне проекта `.env` со значениями:
   ```
   POSTGRES_PASSWORD=...
   MINIO_ROOT_USER=...
   MINIO_ROOT_PASSWORD=...
   ```
2. Проверить, что значения в `src/main/resources/application.properties`
   (`spring.datasource.password`, `minio.access-key`, `minio.secret-key`) совпадают с тем, что задано в `.env`.
3. Запустить приложение:
   ```
   ./mvnw spring-boot:run
   ```
4. Открыть `http://localhost:8080/`.

Бакет `user-files` в MinIO создаётся автоматически при старте приложения, если его ещё нет.

## Тесты

```
./mvnw test
```

Интеграционные тесты поднимают Postgres и MinIO через Testcontainers — для них нужен рабочий Docker.

## Деплой

Деплой — вручную, JAR-артефактом на отдельный сервер; инфраструктура (Postgres, Redis, MinIO) — через Docker Compose
на том же сервере.

1. Собрать jar локально: `./mvnw clean package -DskipTests`
2. На сервере: установить JRE 21 и Docker
3. Скопировать на сервер `compose.yml`, `.env` и собранный jar
4. Поднять инфраструктуру: `docker compose up -d`
5. Запустить jar (рекомендуется — через systemd-сервис, чтобы приложение переживало переподключение по SSH и
   перезагрузку сервера)

Приложение доступно по адресу `http://<server_ip>:8080/`.

Снаружи должен быть открыт только порт `8080` — порты Postgres/MinIO/Redis/Adminer не должны быть доступны из
интернета.
