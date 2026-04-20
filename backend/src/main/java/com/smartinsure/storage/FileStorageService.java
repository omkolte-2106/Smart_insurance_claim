package com.smartinsure.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Abstraction so local disk can be swapped for S3-compatible storage later.
 */
public interface FileStorageService {

    StoredFile storeClaimDocument(Long claimId, MultipartFile file);

    org.springframework.core.io.Resource getClaimDocumentResource(String relativePath);
}
