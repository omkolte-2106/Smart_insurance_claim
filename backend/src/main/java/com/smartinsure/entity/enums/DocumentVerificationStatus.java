package com.smartinsure.entity.enums;

public enum DocumentVerificationStatus {
    PENDING_UPLOAD,
    UPLOADED,
    AI_REVIEWING,
    AI_VERIFIED,
    AI_REJECTED,
    MANUALLY_APPROVED,
    MANUALLY_REJECTED,
    REUPLOAD_REQUESTED
}
