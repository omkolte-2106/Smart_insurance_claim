package com.smartinsure.service;

import com.smartinsure.entity.AppUser;
import com.smartinsure.entity.Claim;
import com.smartinsure.entity.CompanyProfile;
import com.smartinsure.entity.CustomerProfile;
import com.smartinsure.entity.FraudFlag;
import com.smartinsure.entity.enums.CompanyApprovalStatus;
import com.smartinsure.entity.enums.FraudFlagSource;
import com.smartinsure.entity.enums.UserRole;
import com.smartinsure.exception.ApiException;
import com.smartinsure.repository.AppUserRepository;
import com.smartinsure.repository.ClaimRepository;
import com.smartinsure.repository.CompanyProfileRepository;
import com.smartinsure.repository.CustomerProfileRepository;
import com.smartinsure.repository.FraudFlagRepository;
import com.smartinsure.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final CompanyProfileRepository companyProfileRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final AppUserRepository appUserRepository;
    private final FraudFlagRepository fraudFlagRepository;
    private final ClaimRepository claimRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public Page<CompanyProfile> pendingCompanies(Pageable pageable) {
        return companyProfileRepository.findByApprovalStatus(CompanyApprovalStatus.PENDING, pageable);
    }

    @Transactional
    public void approveCompany(SecurityUser admin, Long companyId) {
        assertAdmin(admin);
        CompanyProfile company = companyProfileRepository.findById(companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Company not found"));
        company.setApprovalStatus(CompanyApprovalStatus.APPROVED);
        auditService.record(appUserRepository.findById(admin.getId()).orElse(null),
                "COMPANY_APPROVED", "CompanyProfile", String.valueOf(companyId), company.getLegalName());
        notificationService.notify(company.getUser(),
                "Registration approved",
                "You can now sign in to the SmartInsure company console.");
    }

    @Transactional
    public void rejectCompany(SecurityUser admin, Long companyId, String reason) {
        assertAdmin(admin);
        CompanyProfile company = companyProfileRepository.findById(companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Company not found"));
        company.setApprovalStatus(CompanyApprovalStatus.REJECTED);
        auditService.record(appUserRepository.findById(admin.getId()).orElse(null),
                "COMPANY_REJECTED", "CompanyProfile", String.valueOf(companyId), reason);
        notificationService.notify(company.getUser(),
                "Registration rejected",
                reason != null ? reason : "Please contact support for more information.");
    }

    @Transactional
    public void banCustomer(SecurityUser admin, Long customerProfileId, String reason) {
        assertAdmin(admin);
        CustomerProfile cp = customerProfileRepository.findById(customerProfileId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Customer not found"));
        cp.setBanned(true);
        cp.setBanReason(reason);
        cp.getUser().setBanned(true);
        cp.getUser().setBanReason(reason);
        auditService.record(appUserRepository.findById(admin.getId()).orElse(null),
                "CUSTOMER_BANNED", "CustomerProfile", String.valueOf(customerProfileId), reason);
    }

    @Transactional
    public void unbanCustomer(SecurityUser admin, Long customerProfileId) {
        assertAdmin(admin);
        CustomerProfile cp = customerProfileRepository.findById(customerProfileId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Customer not found"));
        cp.setBanned(false);
        cp.setBanReason(null);
        cp.getUser().setBanned(false);
        cp.getUser().setBanReason(null);
        auditService.record(appUserRepository.findById(admin.getId()).orElse(null),
                "CUSTOMER_UNBANNED", "CustomerProfile", String.valueOf(customerProfileId), null);
    }

    @Transactional
    public void banCompany(SecurityUser admin, Long companyId, String reason) {
        assertAdmin(admin);
        CompanyProfile company = companyProfileRepository.findById(companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Company not found"));
        company.setBanned(true);
        company.setBanReason(reason);
        company.getUser().setBanned(true);
        company.getUser().setBanReason(reason);
        auditService.record(appUserRepository.findById(admin.getId()).orElse(null),
                "COMPANY_BANNED", "CompanyProfile", String.valueOf(companyId), reason);
    }

    @Transactional
    public void unbanCompany(SecurityUser admin, Long companyId) {
        assertAdmin(admin);
        CompanyProfile company = companyProfileRepository.findById(companyId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Company not found"));
        company.setBanned(false);
        company.setBanReason(null);
        company.getUser().setBanned(false);
        company.getUser().setBanReason(null);
        auditService.record(appUserRepository.findById(admin.getId()).orElse(null),
                "COMPANY_UNBANNED", "CompanyProfile", String.valueOf(companyId), null);
    }

    @Transactional(readOnly = true)
    public Page<FraudFlag> activeFraudFlags(Pageable pageable) {
        return fraudFlagRepository.findByActiveTrue(pageable);
    }

    @Transactional
    public void raiseFraudFlag(SecurityUser admin, String claimPublicId, String description) {
        assertAdmin(admin);
        Claim claim = claimRepository.findByClaimPublicIdIgnoreCase(claimPublicId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Claim not found"));
        AppUser actor = appUserRepository.findById(admin.getId()).orElseThrow();
        FraudFlag flag = FraudFlag.builder()
                .claim(claim)
                .raisedBy(actor)
                .source(FraudFlagSource.ADMIN)
                .description(description)
                .active(true)
                .build();
        fraudFlagRepository.save(flag);
        claim.setFraudFlagged(true);
        auditService.record(actor, "FRAUD_FLAG_ADMIN", "Claim", claimPublicId, description);
        notificationService.notify(claim.getCompany().getUser(),
                "Fraud flag raised",
                "Admin flagged claim " + claimPublicId);
    }

    private void assertAdmin(SecurityUser user) {
        if (user.getRole() != UserRole.ROLE_ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin only");
        }
    }
}
