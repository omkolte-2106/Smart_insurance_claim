package com.smartinsure.controller;

import com.smartinsure.entity.enums.CompanyApprovalStatus;
import com.smartinsure.repository.ClaimRepository;
import com.smartinsure.repository.CompanyProfileRepository;
import com.smartinsure.repository.CustomerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final CustomerProfileRepository customerProfileRepository;
    private final CompanyProfileRepository companyProfileRepository;
    private final ClaimRepository claimRepository;

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return Map.of(
                "customers", customerProfileRepository.count(),
                "approvedInsurers", companyProfileRepository.findAll().stream()
                        .filter(c -> c.getApprovalStatus() == CompanyApprovalStatus.APPROVED).count(),
                "claimsProcessed", claimRepository.count()
        );
    }
}
