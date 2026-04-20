package com.smartinsure.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinsure.dto.claim.*;
import com.smartinsure.entity.*;
import com.smartinsure.entity.enums.*;
import com.smartinsure.exception.ApiException;
import com.smartinsure.ml.MlServiceClient;
import com.smartinsure.ml.dto.*;
import com.smartinsure.repository.*;
import com.smartinsure.security.SecurityUser;
import com.smartinsure.storage.FileStorageService;
import com.smartinsure.util.ClaimIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClaimService {

    public static final String MODULE_DOCUMENT = "document_verification_service";
    public static final String MODULE_FRAUD = "fraud_detection_service";
    public static final String MODULE_DAMAGE = "damage_severity_service";
    public static final String MODULE_PAYOUT = "payout_estimation_service";

    private static final Set<ClaimDocumentType> REQUIRED_DOCS = EnumSet.of(
            ClaimDocumentType.AADHAAR,
            ClaimDocumentType.DRIVING_LICENCE,
            ClaimDocumentType.PUC_CERTIFICATE,
            ClaimDocumentType.VEHICLE_DAMAGE_PHOTO
    );

    private final ClaimRepository claimRepository;
    private final InsurancePolicyRepository policyRepository;
    private final ClaimDocumentRepository claimDocumentRepository;
    private final VerificationResultRepository verificationResultRepository;
    private final PayoutRepository payoutRepository;
    private final FraudFlagRepository fraudFlagRepository;
    private final ClaimRemarkRepository claimRemarkRepository;
    private final FileStorageService fileStorageService;
    private final MlServiceClient mlServiceClient;
    private final ObjectMapper objectMapper;
    private final ClaimIdGenerator claimIdGenerator;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final AppUserRepository appUserRepository;

    @Transactional
    public ClaimSummaryDto createClaim(SecurityUser user, CreateClaimRequest req) {
        if (user.getCustomerProfileId() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Customer profile required");
        }
        InsurancePolicy policy = policyRepository.findById(req.getPolicyId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Policy not found"));
        if (!policy.getCustomer().getId().equals(user.getCustomerProfileId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Policy does not belong to customer");
        }
        if (policy.getStatus() != PolicyStatus.ACTIVE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Policy is not active");
        }
        Claim claim = Claim.builder()
                .claimPublicId(claimIdGenerator.next())
                .policy(policy)
                .customer(policy.getCustomer())
                .company(policy.getCompany())
                .status(ClaimStatus.SUBMITTED)
                .incidentDescription(req.getIncidentDescription())
                .incidentLocation(req.getIncidentLocation())
                .fraudFlagged(false)
                .build();
        claimRepository.save(claim);
        auditService.record(appUserRepository.findById(user.getId()).orElse(null),
                "CLAIM_CREATED", "Claim", claim.getClaimPublicId(), "Customer filed claim");
        notificationService.notify(policy.getCompany().getUser(),
                "New claim submitted",
                "Claim " + claim.getClaimPublicId() + " was filed for policy " + policy.getPolicyNumber());
        return toSummary(claim);
    }

    @Transactional
    public ClaimDocumentDto uploadDocument(SecurityUser user, String claimPublicId, ClaimDocumentType type, MultipartFile file) {
        Claim claim = loadClaimForCustomer(user, claimPublicId);
        if (claim.getStatus() == ClaimStatus.REJECTED || claim.getStatus() == ClaimStatus.SETTLED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot upload documents for this claim state");
        }
        var stored = fileStorageService.storeClaimDocument(claim.getId(), file);
        ClaimDocument doc = ClaimDocument.builder()
                .claim(claim)
                .documentType(type)
                .originalFilename(stored.originalFilename())
                .storedPath(stored.relativePath())
                .contentType(stored.contentType())
                .sizeBytes(stored.sizeBytes())
                .verificationStatus(DocumentVerificationStatus.UPLOADED)
                .build();
        claimDocumentRepository.save(doc);
        if (claim.getStatus() == ClaimStatus.SUBMITTED) {
            claim.setStatus(ClaimStatus.DOCUMENTS_UPLOADED);
        }
        if (claim.getStatus() == ClaimStatus.ADDITIONAL_DOCUMENTS_REQUIRED) {
            claim.setStatus(ClaimStatus.DOCUMENTS_UPLOADED);
        }
        return toDocDto(doc);
    }

    @Transactional
    public ClaimSummaryDto submitDocumentsForAi(SecurityUser user, String claimPublicId) {
        Claim claim = loadClaimForCustomer(user, claimPublicId);
        List<ClaimDocument> docs = claimDocumentRepository.findByClaimId(claim.getId());
        Set<ClaimDocumentType> present = docs.stream()
                .filter(d -> d.getVerificationStatus() != DocumentVerificationStatus.REUPLOAD_REQUESTED)
                .map(ClaimDocument::getDocumentType)
                .collect(Collectors.toSet());
        for (ClaimDocumentType required : REQUIRED_DOCS) {
            if (!present.contains(required)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Missing required document: " + required);
            }
        }
        runAiPipeline(claim);
        return toSummary(claimRepository.findById(claim.getId()).orElseThrow());
    }

    @Transactional
    public void runAiPipeline(Claim claim) {
        claim.setStatus(ClaimStatus.AI_VERIFICATION_IN_PROGRESS);

        Map<String, Object> docPayload = Map.of(
                "claimPublicId", claim.getClaimPublicId(),
                "documentCount", claimDocumentRepository.findByClaimId(claim.getId()).size()
        );
        MlDocumentVerificationResponse docResp = mlServiceClient.verifyDocuments(docPayload);
        persistVerification(claim, MODULE_DOCUMENT, docResp);
        
        // Real document verification logic: if validity < 0.5, mark as re-upload requested
        claimDocumentRepository.findByClaimId(claim.getId()).forEach(d -> {
            if (docResp.validityScore() < 0.5) {
                d.setVerificationStatus(DocumentVerificationStatus.REUPLOAD_REQUESTED);
                d.setRejectionReason(docResp.notes());
            } else {
                d.setVerificationStatus(DocumentVerificationStatus.AI_VERIFIED);
            }
        });

        Map<String, Object> fraudPayload = Map.of("claimPublicId", claim.getClaimPublicId());
        MlFraudResponse fraudResp = mlServiceClient.scoreFraud(fraudPayload);
        persistVerification(claim, MODULE_FRAUD, fraudResp);
        claim.setFraudScore(fraudResp.fraudScore() * 100);

        Map<String, Object> damagePayload = Map.of("claimPublicId", claim.getClaimPublicId(), "imageCount",
                claimDocumentRepository.findByClaimId(claim.getId()).stream()
                        .filter(d -> d.getDocumentType() == ClaimDocumentType.VEHICLE_DAMAGE_PHOTO).count());
        MlDamageSeverityResponse damageResp = mlServiceClient.predictDamage(damagePayload);
        persistVerification(claim, MODULE_DAMAGE, damageResp);
        claim.setDamageSeverityScore(damageResp.severityScore());

        // NEW: Detect damaged parts
        MlDamagePartsResponse partsResp = mlServiceClient.detectPartsDamage(damagePayload);
        if (partsResp.detectedParts() != null) {
            claim.setDamagedParts(String.join(",", partsResp.detectedParts()));
        }

        Map<String, Object> payoutPayload = Map.of(
                "claimPublicId", claim.getClaimPublicId(),
                "severity", damageResp.severityScore(),
                "sumInsured", claim.getPolicy().getSumInsured()
        );
        MlPayoutEstimateResponse payoutResp = mlServiceClient.estimatePayout(payoutPayload);
        persistVerification(claim, MODULE_PAYOUT, payoutResp);
        claim.setEstimatedPayoutAmount(payoutResp.recommendedPayout());

        Payout payout = Payout.builder()
                .claim(claim)
                .estimatedAmount(payoutResp.recommendedPayout())
                .recommendedAmount(payoutResp.recommendedPayout())
                .status(PayoutStatus.RECOMMENDED)
                .notes(payoutResp.rationale())
                .build();
        payoutRepository.save(payout);
        claim.setPayout(payout);

        if (fraudResp.fraudScore() > 0.65) {
            claim.setFraudFlagged(true);
            claim.setStatus(ClaimStatus.FRAUD_FLAGGED);
            FraudFlag flag = FraudFlag.builder()
                    .claim(claim)
                    .source(FraudFlagSource.AI)
                    .description("Automated fraud suspicion score=" + fraudResp.fraudScore())
                    .active(true)
                    .build();
            fraudFlagRepository.save(flag);
        } else {
            claim.setStatus(ClaimStatus.MANUAL_REVIEW_PENDING);
        }

        // Removed universal verification; already handled above

        notificationService.notify(claim.getCustomer().getUser(),
                "AI verification complete",
                "Claim " + claim.getClaimPublicId() + " moved to insurer review.");
        notificationService.notify(claim.getCompany().getUser(),
                "Claim ready for manual review",
                "Claim " + claim.getClaimPublicId() + " is awaiting verification.");
    }

    private void persistVerification(Claim claim, String module, Object payload) {
        try {
            VerificationResult vr = VerificationResult.builder()
                    .claim(claim)
                    .moduleName(module)
                    .payloadJson(objectMapper.writeValueAsString(payload))
                    .score(extractScore(payload))
                    .build();
            verificationResultRepository.save(vr);
        } catch (JsonProcessingException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to persist verification payload");
        }
    }

    private Double extractScore(Object payload) {
        return switch (payload) {
            case MlDocumentVerificationResponse d -> d.validityScore();
            case MlFraudResponse f -> f.fraudScore();
            case MlDamageSeverityResponse s -> s.severityScore();
            case MlPayoutEstimateResponse p -> p.recommendedPayout() != null ? p.recommendedPayout().doubleValue() : null;
            default -> null;
        };
    }

    @Transactional(readOnly = true)
    public ClaimSummaryDto getClaimForActor(SecurityUser user, String claimPublicId) {
        Claim claim = claimRepository.findByClaimPublicIdIgnoreCase(claimPublicId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Claim not found"));
        assertAccess(user, claim);
        return toSummary(claim);
    }

    @Transactional(readOnly = true)
    public Page<ClaimSummaryDto> listFraudClaimsForAdmin(SecurityUser user, Pageable pageable) {
        if (user.getRole() != UserRole.ROLE_ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin only");
        }
        return claimRepository.findByFraudFlaggedTrue(pageable).map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public Page<ClaimSummaryDto> listForActor(SecurityUser user, Pageable pageable) {
        if (user.getRole() == UserRole.ROLE_ADMIN) {
            return claimRepository.findAll(pageable).map(this::toSummary);
        }
        if (user.getRole() == UserRole.ROLE_COMPANY) {
            return claimRepository.findByCompanyId(user.getCompanyProfileId(), pageable).map(this::toSummary);
        }
        if (user.getRole() == UserRole.ROLE_CUSTOMER) {
            return claimRepository.findByCustomerId(user.getCustomerProfileId(), pageable).map(this::toSummary);
        }
        return Page.empty(pageable);
    }

    @Transactional
    public ClaimSummaryDto companyDecision(SecurityUser user, String claimPublicId, ClaimStatus target, String remarks) {
        if (user.getRole() != UserRole.ROLE_COMPANY) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Company role required");
        }
        Claim claim = claimRepository.findByClaimPublicIdIgnoreCase(claimPublicId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Claim not found"));
        if (!claim.getCompany().getId().equals(user.getCompanyProfileId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Claim not owned by company");
        }
        if (claim.getStatus() != ClaimStatus.MANUAL_REVIEW_PENDING
                && claim.getStatus() != ClaimStatus.FRAUD_FLAGGED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Claim is not awaiting manual review");
        }
        AppUser actor = appUserRepository.findById(user.getId()).orElseThrow();
        switch (target) {
            case APPROVED -> claim.setStatus(ClaimStatus.APPROVED);
            case REJECTED -> claim.setStatus(ClaimStatus.REJECTED);
            case ADDITIONAL_DOCUMENTS_REQUIRED -> {
                claim.setStatus(ClaimStatus.ADDITIONAL_DOCUMENTS_REQUIRED);
                claimDocumentRepository.findByClaimId(claim.getId()).forEach(d ->
                        d.setVerificationStatus(DocumentVerificationStatus.REUPLOAD_REQUESTED));
            }
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported company decision status");
        }
        if (remarks != null && !remarks.isBlank()) {
            ClaimRemark remark = ClaimRemark.builder()
                    .claim(claim)
                    .author(actor)
                    .remarkText(remarks)
                    .build();
            claimRemarkRepository.save(remark);
        }
        auditService.record(actor, "COMPANY_DECISION", "Claim", claim.getClaimPublicId(), target.name());
        notificationService.notify(claim.getCustomer().getUser(),
                "Claim updated",
                "Your claim " + claim.getClaimPublicId() + " status is now " + claim.getStatus());
        return toSummary(claim);
    }

    @Transactional
    public ClaimSummaryDto settleClaim(SecurityUser user, String claimPublicId, BigDecimal finalAmount) {
        if (user.getRole() != UserRole.ROLE_COMPANY && user.getRole() != UserRole.ROLE_ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed");
        }
        Claim claim = claimRepository.findByClaimPublicIdIgnoreCase(claimPublicId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Claim not found"));
        if (user.getRole() == UserRole.ROLE_COMPANY
                && !claim.getCompany().getId().equals(user.getCompanyProfileId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Claim not owned by company");
        }
        if (claim.getStatus() != ClaimStatus.APPROVED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Claim must be approved before settlement");
        }
        Payout payout = claim.getPayout();
        if (payout == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Payout missing");
        }
        payout.setFinalAmount(finalAmount != null ? finalAmount : payout.getRecommendedAmount());
        payout.setStatus(PayoutStatus.SETTLED);
        claim.setStatus(ClaimStatus.SETTLED);
        auditService.record(appUserRepository.findById(user.getId()).orElse(null),
                "CLAIM_SETTLED", "Claim", claim.getClaimPublicId(), payout.getFinalAmount().toPlainString());
        notificationService.notify(claim.getCustomer().getUser(),
                "Claim settled",
                "Payout for " + claim.getClaimPublicId() + " has been recorded.");
        return toSummary(claim);
    }

    private Claim loadClaimForCustomer(SecurityUser user, String claimPublicId) {
        Claim claim = claimRepository.findByClaimPublicIdIgnoreCase(claimPublicId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Claim not found"));
        if (!claim.getCustomer().getId().equals(user.getCustomerProfileId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Claim does not belong to customer");
        }
        return claim;
    }

    private void assertAccess(SecurityUser user, Claim claim) {
        switch (user.getRole()) {
            case ROLE_ADMIN -> { /* ok */ }
            case ROLE_COMPANY -> {
                if (!claim.getCompany().getId().equals(user.getCompanyProfileId())) {
                    throw new ApiException(HttpStatus.FORBIDDEN, "No access to claim");
                }
            }
            case ROLE_CUSTOMER -> {
                if (!claim.getCustomer().getId().equals(user.getCustomerProfileId())) {
                    throw new ApiException(HttpStatus.FORBIDDEN, "No access to claim");
                }
            }
        }
    }

    private ClaimSummaryDto toSummary(Claim claim) {
        List<ClaimDocumentDto> docs = claimDocumentRepository.findByClaimId(claim.getId()).stream()
                .map(this::toDocDto)
                .toList();
        return ClaimSummaryDto.builder()
                .id(claim.getId())
                .claimPublicId(claim.getClaimPublicId())
                .status(claim.getStatus())
                .incidentDescription(claim.getIncidentDescription())
                .fraudScore(claim.getFraudScore())
                .damageSeverityScore(claim.getDamageSeverityScore())
                .estimatedPayoutAmount(claim.getEstimatedPayoutAmount())
                .fraudFlagged(claim.isFraudFlagged())
                .companyName(claim.getCompany().getLegalName())
                .policyNumber(claim.getPolicy().getPolicyNumber())
                .vehicleRegistration(claim.getPolicy().getVehicle().getRegistrationNumber())
                .createdAt(claim.getCreatedAt())
                .documents(docs)
                .damagedParts(claim.getDamagedParts() != null ? 
                        Arrays.stream(claim.getDamagedParts().split(","))
                                .filter(s -> !s.isBlank())
                                .toList() : List.of())
                .build();
    }

    private ClaimDocumentDto toDocDto(ClaimDocument d) {
        return ClaimDocumentDto.builder()
                .id(d.getId())
                .documentType(d.getDocumentType())
                .originalFilename(d.getOriginalFilename())
                .verificationStatus(d.getVerificationStatus())
                .rejectionReason(d.getRejectionReason())
                .build();
    }

    @Transactional(readOnly = true)
    public String exportClaimSummary(SecurityUser user, String claimPublicId) {
        ClaimSummaryDto dto = getClaimForActor(user, claimPublicId);
        StringBuilder sb = new StringBuilder();
        sb.append("SmartInsure Claim Summary\n");
        sb.append("Claim ID: ").append(dto.getClaimPublicId()).append("\n");
        sb.append("Status: ").append(dto.getStatus()).append("\n");
        sb.append("Insurer: ").append(dto.getCompanyName()).append("\n");
        sb.append("Policy: ").append(dto.getPolicyNumber()).append("\n");
        sb.append("Vehicle: ").append(dto.getVehicleRegistration()).append("\n");
        sb.append("Fraud score: ").append(dto.getFraudScore()).append("\n");
        sb.append("Damage severity: ").append(dto.getDamageSeverityScore()).append("\n");
        sb.append("Estimated payout: ").append(dto.getEstimatedPayoutAmount()).append("\n");
        sb.append("Documents:\n");
        dto.getDocuments().forEach(doc ->
                sb.append(" - ").append(doc.getDocumentType()).append(" (").append(doc.getVerificationStatus()).append(")\n"));
        return sb.toString();
    }
}
