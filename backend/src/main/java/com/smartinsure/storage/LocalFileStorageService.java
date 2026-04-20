package com.smartinsure.storage;

import com.smartinsure.config.SmartInsureProperties;
import com.smartinsure.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocalFileStorageService implements FileStorageService {

    private static final long MAX_BYTES = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXT = Set.of("pdf", "jpg", "jpeg", "png");

    private final SmartInsureProperties properties;

    @Override
    public StoredFile storeClaimDocument(Long claimId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File is required");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "File exceeds maximum size of 10MB");
        }
        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename());
        String ext = extension(original).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported file type. Allowed: PDF, JPG, PNG");
        }

        Path root = Paths.get(properties.getStorage().getRootPath()).toAbsolutePath().normalize();
        Path targetDir = root.resolve("claims").resolve(String.valueOf(claimId));
        try {
            Files.createDirectories(targetDir);
            String storedName = UUID.randomUUID() + "-" + original.replace(" ", "_");
            Path target = targetDir.resolve(storedName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            String relative = Paths.get("claims", String.valueOf(claimId), storedName).toString().replace("\\", "/");
            return new StoredFile(relative, original, file.getContentType(), file.getSize());
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store file");
        }
    }

    private String extension(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx == -1 ? "" : filename.substring(idx + 1);
    }
}
