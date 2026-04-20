package com.smartinsure.controller;

import com.smartinsure.dto.claim.ClaimDocumentDto;
import com.smartinsure.dto.claim.ClaimSummaryDto;
import com.smartinsure.dto.claim.CreateClaimRequest;
import com.smartinsure.entity.enums.ClaimDocumentType;
import com.smartinsure.service.ClaimService;
import com.smartinsure.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;
    private final CurrentUserService currentUserService;

    @PostMapping
    public ClaimSummaryDto create(@Valid @RequestBody CreateClaimRequest request) {
        return claimService.createClaim(currentUserService.currentUser(), request);
    }

    @PostMapping(value = "/{claimPublicId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ClaimDocumentDto upload(@PathVariable String claimPublicId,
                                   @RequestParam ClaimDocumentType type,
                                   @RequestPart("file") MultipartFile file) {
        return claimService.uploadDocument(currentUserService.currentUser(), claimPublicId, type, file);
    }

    @PostMapping("/{claimPublicId}/submit-ai")
    public ClaimSummaryDto submitAi(@PathVariable String claimPublicId) {
        return claimService.submitDocumentsForAi(currentUserService.currentUser(), claimPublicId);
    }

    @GetMapping("/{claimPublicId}")
    public ClaimSummaryDto get(@PathVariable String claimPublicId) {
        return claimService.getClaimForActor(currentUserService.currentUser(), claimPublicId);
    }

    @GetMapping
    public Page<ClaimSummaryDto> list(Pageable pageable) {
        return claimService.listForActor(currentUserService.currentUser(), pageable);
    }

    @GetMapping(value = "/{claimPublicId}/export", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> export(@PathVariable String claimPublicId) {
        String body = claimService.exportClaimSummary(currentUserService.currentUser(), claimPublicId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + claimPublicId + ".txt\"")
                .body(body);
    }

    @GetMapping("/{claimPublicId}/documents/{documentId}/content")
    public ResponseEntity<org.springframework.core.io.Resource> getDocumentContent(
            @PathVariable String claimPublicId,
            @PathVariable Long documentId) {
        org.springframework.core.io.Resource resource = claimService.getDocumentResource(
                currentUserService.currentUser(), claimPublicId, documentId);
        
        String contentType = "application/octet-stream";
        try {
            contentType = java.nio.file.Files.probeContentType(java.nio.file.Paths.get(resource.getURI()));
        } catch (Exception ignored) {}

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
