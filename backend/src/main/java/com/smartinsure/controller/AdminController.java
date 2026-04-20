package com.smartinsure.controller;

import com.smartinsure.dto.admin.AdminDashboardDto;
import com.smartinsure.entity.CompanyProfile;
import com.smartinsure.entity.CustomerProfile;
import com.smartinsure.entity.DiscountEligibility;
import com.smartinsure.entity.FraudFlag;
import com.smartinsure.entity.AuditLog;
import com.smartinsure.dto.claim.ClaimSummaryDto;
import com.smartinsure.service.AdminService;
import com.smartinsure.service.AnalyticsService;
import com.smartinsure.service.ClaimService;
import com.smartinsure.service.CurrentUserService;
import com.smartinsure.service.DiscountService;
import com.smartinsure.repository.AuditLogRepository;
import com.smartinsure.repository.CustomerProfileRepository;
import com.smartinsure.repository.CompanyProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AnalyticsService analyticsService;
    private final AdminService adminService;
    private final DiscountService discountService;
    private final CurrentUserService currentUserService;
    private final AuditLogRepository auditLogRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final CompanyProfileRepository companyProfileRepository;
    private final ClaimService claimService;

    @GetMapping("/dashboard")
    public AdminDashboardDto dashboard() {
        currentUserService.currentUser(); // ensure authenticated
        return analyticsService.adminDashboard();
    }

    @GetMapping("/companies/pending")
    public Page<CompanyProfile> pendingCompanies(Pageable pageable) {
        return adminService.pendingCompanies(pageable);
    }

    @PostMapping("/companies/{id}/approve")
    public void approveCompany(@PathVariable Long id) {
        adminService.approveCompany(currentUserService.currentUser(), id);
    }

    @PostMapping("/companies/{id}/reject")
    public void rejectCompany(@PathVariable Long id, @RequestParam(required = false) String reason) {
        adminService.rejectCompany(currentUserService.currentUser(), id, reason);
    }

    @PostMapping("/companies/{id}/ban")
    public void banCompany(@PathVariable Long id, @RequestParam(required = false) String reason) {
        adminService.banCompany(currentUserService.currentUser(), id, reason);
    }

    @PostMapping("/companies/{id}/unban")
    public void unbanCompany(@PathVariable Long id) {
        adminService.unbanCompany(currentUserService.currentUser(), id);
    }

    @PostMapping("/customers/{id}/ban")
    public void banCustomer(@PathVariable Long id, @RequestParam(required = false) String reason) {
        adminService.banCustomer(currentUserService.currentUser(), id, reason);
    }

    @PostMapping("/customers/{id}/unban")
    public void unbanCustomer(@PathVariable Long id) {
        adminService.unbanCustomer(currentUserService.currentUser(), id);
    }

    @GetMapping("/fraud/flags")
    public Page<FraudFlag> fraudFlags(Pageable pageable) {
        return adminService.activeFraudFlags(pageable);
    }

    @GetMapping("/fraud/claims")
    public Page<ClaimSummaryDto> fraudClaims(Pageable pageable) {
        return claimService.listFraudClaimsForAdmin(currentUserService.currentUser(), pageable);
    }

    @PostMapping("/claims/{claimPublicId}/fraud-flags")
    public void raiseFraud(@PathVariable String claimPublicId, @RequestParam String description) {
        adminService.raiseFraudFlag(currentUserService.currentUser(), claimPublicId, description);
    }

    @GetMapping("/audit-logs")
    public Page<AuditLog> auditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @GetMapping("/customers")
    public Page<CustomerProfile> customers(Pageable pageable) {
        return customerProfileRepository.findAll(pageable);
    }

    @GetMapping("/companies")
    public Page<CompanyProfile> companies(Pageable pageable) {
        return companyProfileRepository.findAll(pageable);
    }

    @PostMapping("/discounts/recompute")
    public List<DiscountEligibility> recomputeDiscounts(@RequestParam(defaultValue = "0.15") double topFraction) {
        return discountService.recomputeTopCustomers(currentUserService.currentUser(), topFraction);
    }
}
