package com.smartinsure.dto.claim;

import com.smartinsure.entity.enums.ClaimStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Value
@Builder
public class ClaimSummaryDto {
    Long id;
    String claimPublicId;
    ClaimStatus status;
    String incidentDescription;
    Double fraudScore;
    Double damageSeverityScore;
    BigDecimal estimatedPayoutAmount;
    boolean fraudFlagged;
    String companyName;
    String policyNumber;
    String vehicleRegistration;
    Instant createdAt;
    List<ClaimDocumentDto> documents;
    List<String> damagedParts;
}
