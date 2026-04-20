package com.smartinsure.service;

import com.smartinsure.dto.admin.AdminDashboardDto;
import com.smartinsure.entity.CompanyProfile;
import com.smartinsure.entity.CustomerProfile;
import com.smartinsure.entity.enums.ClaimStatus;
import com.smartinsure.entity.enums.CompanyApprovalStatus;
import com.smartinsure.entity.enums.UserRole;
import com.smartinsure.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AppUserRepository appUserRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final CompanyProfileRepository companyProfileRepository;
    private final ClaimRepository claimRepository;
    private final DiscountEligibilityRepository discountEligibilityRepository;

    @Transactional(readOnly = true)
    public AdminDashboardDto adminDashboard() {
        long customers = customerProfileRepository.count();
        long companies = companyProfileRepository.count();
        long claims = claimRepository.count();
        long pendingCompanies = companyProfileRepository.countByApprovalStatus(CompanyApprovalStatus.PENDING);
        long pendingClaims = claimRepository.findAll().stream()
                .filter(c -> c.getStatus() == ClaimStatus.MANUAL_REVIEW_PENDING
                        || c.getStatus() == ClaimStatus.FRAUD_FLAGGED)
                .count();
        long fraudClaims = claimRepository.countByFraudFlaggedTrue();
        long bannedCustomers = customerProfileRepository.findAll().stream().filter(CustomerProfile::isBanned).count();
        long bannedCompanies = companyProfileRepository.findAll().stream().filter(CompanyProfile::isBanned).count();
        long discountEligible = discountEligibilityRepository.count();
        return AdminDashboardDto.builder()
                .totalCustomers(customers)
                .totalCompanies(companies)
                .totalClaims(claims)
                .pendingCompanyApprovals(pendingCompanies)
                .pendingClaims(pendingClaims)
                .fraudFlaggedClaims(fraudClaims)
                .bannedCustomers(bannedCustomers)
                .bannedCompanies(bannedCompanies)
                .discountEligibleCustomers(discountEligible)
                .build();
    }

    @Transactional(readOnly = true)
    public long countUsersByRole(UserRole role) {
        return appUserRepository.countByRole(role);
    }
}
