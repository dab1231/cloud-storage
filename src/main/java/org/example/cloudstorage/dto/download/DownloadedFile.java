package org.example.cloudstorage.dto.download;

import java.io.InputStream;

public record DownloadedFile(String name, InputStream inputStream) {
}
