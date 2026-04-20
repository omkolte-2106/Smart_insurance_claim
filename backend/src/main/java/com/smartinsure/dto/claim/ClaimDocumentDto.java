package com.smartinsure.dto.claim;

import com.smartinsure.entity.enums.ClaimDocumentType;
import com.smartinsure.entity.enums.DocumentVerificationStatus;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ClaimDocumentDto {
    Long id;
    ClaimDocumentType documentType;
    String originalFilename;
    DocumentVerificationStatus verificationStatus;
    String rejectionReason;
}
