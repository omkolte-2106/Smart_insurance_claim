package com.smartinsure.controller;

import com.smartinsure.dto.company.BulkPolicyResponse;
import com.smartinsure.dto.company.CompanyDashboardDto;
import com.smartinsure.dto.company.CreatePolicyRequest;
import com.smartinsure.dto.company.SettleClaimRequest;
import com.smartinsure.dto.claim.ClaimSummaryDto;
import com.smartinsure.dto.claim.CompanyClaimActionRequest;
import com.smartinsure.entity.InsurancePolicy;
import com.smartinsure.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CurrentUserService currentUserService;
    private final CompanyAnalyticsService companyAnalyticsService;
    private final PolicyService policyService;
    private final ClaimService claimService;

    @GetMapping("/dashboard")
    public CompanyDashboardDto dashboard() {
        return companyAnalyticsService.dashboard(currentUserService.currentUser());
    }

    @PostMapping("/policies")
    public InsurancePolicy createPolicy(@Valid @RequestBody CreatePolicyRequest request) {
        return policyService.createPolicyForCompany(currentUserService.currentUser(), request);
    }

    @PostMapping(value = "/policies/bulk")
    public BulkPolicyResponse bulkCreatePolicies(@RequestParam("file") MultipartFile file) {
        return policyService.bulkIssuePolicies(currentUserService.currentUser(), file);
    }

    @GetMapping(value = "/policies/csv-template", produces = "text/csv")
    public String downloadCsvTemplate(@RequestParam(value = "prefill", defaultValue = "false") boolean prefill) {
        return policyService.generatePolicyTemplateCsv(prefill);
    }

    @PostMapping("/claims/{claimPublicId}/decision")
    public ClaimSummaryDto decision(@PathVariable String claimPublicId,
                                   @Valid @RequestBody CompanyClaimActionRequest request) {
        return claimService.companyDecision(currentUserService.currentUser(), claimPublicId,
                request.getTargetStatus(), request.getRemarks());
    }

    @PostMapping("/claims/{claimPublicId}/settle")
    public ClaimSummaryDto settle(@PathVariable String claimPublicId,
                                  @Valid @RequestBody SettleClaimRequest request) {
        return claimService.settleClaim(currentUserService.currentUser(), claimPublicId, request.getFinalAmount());
    }
}
