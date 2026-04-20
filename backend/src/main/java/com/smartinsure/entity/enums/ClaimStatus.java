package com.smartinsure.entity.enums;

/**
 * Vehicle claim lifecycle. Order reflects typical progression but not all paths apply.
 */
public enum ClaimStatus {
    SUBMITTED,
    DOCUMENTS_UPLOADED,
    AI_VERIFICATION_IN_PROGRESS,
    AI_VERIFIED,
    MANUAL_REVIEW_PENDING,
    ADDITIONAL_DOCUMENTS_REQUIRED,
    APPROVED,
    REJECTED,
    FRAUD_FLAGGED,
    PAYOUT_CALCULATED,
    SETTLED
}
