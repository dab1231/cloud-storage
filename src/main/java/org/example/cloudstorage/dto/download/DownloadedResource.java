package org.example.cloudstorage.dto.download;

import java.io.InputStream;

public record DownloadedResource(String name, InputStream inputStream) {
}
