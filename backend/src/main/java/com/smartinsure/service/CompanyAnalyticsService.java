package com.smartinsure.service;

import com.smartinsure.dto.company.CompanyDashboardDto;
import com.smartinsure.entity.Claim;
import com.smartinsure.entity.enums.ClaimStatus;
import com.smartinsure.entity.enums.UserRole;
import com.smartinsure.exception.ApiException;
import com.smartinsure.repository.ClaimRepository;
import com.smartinsure.repository.InsurancePolicyRepository;
import com.smartinsure.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyAnalyticsService {

    private final InsurancePolicyRepository insurancePolicyRepository;
    private final ClaimRepository claimRepository;

    @Transactional(readOnly = true)
    public CompanyDashboardDto dashboard(SecurityUser user) {
        if (user.getRole() != UserRole.ROLE_COMPANY) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Company role required");
        }
        long companyId = user.getCompanyProfileId();
        long policyholders = insurancePolicyRepository.findByCompanyId(companyId).stream()
                .map(p -> p.getCustomer().getId())
                .distinct()
                .count();
        List<Claim> claims = claimRepository.findByCompanyId(companyId, Pageable.unpaged()).getContent();
        long totalClaims = claims.size();
        long pendingManual = claims.stream()
                .filter(c -> c.getStatus() == ClaimStatus.MANUAL_REVIEW_PENDING
                        || c.getStatus() == ClaimStatus.FRAUD_FLAGGED)
                .count();
        long approved = claims.stream().filter(c -> c.getStatus() == ClaimStatus.APPROVED).count();
        long rejected = claims.stream().filter(c -> c.getStatus() == ClaimStatus.REJECTED).count();
        BigDecimal estPayout = claims.stream()
                .map(Claim::getEstimatedPayoutAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CompanyDashboardDto(policyholders, totalClaims, pendingManual, approved, rejected, estPayout);
    }
}
