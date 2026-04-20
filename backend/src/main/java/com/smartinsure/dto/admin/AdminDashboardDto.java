package com.smartinsure.dto.admin;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AdminDashboardDto {
    long totalCustomers;
    long totalCompanies;
    long totalClaims;
    long pendingCompanyApprovals;
    long pendingClaims;
    long fraudFlaggedClaims;
    long bannedCustomers;
    long bannedCompanies;
    long discountEligibleCustomers;
}
