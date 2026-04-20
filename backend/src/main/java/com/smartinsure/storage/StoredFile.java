package com.smartinsure.storage;

public record StoredFile(String relativePath, String originalFilename, String contentType, long sizeBytes) {
}
