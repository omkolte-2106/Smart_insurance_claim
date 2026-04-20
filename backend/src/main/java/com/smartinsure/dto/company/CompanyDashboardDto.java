package com.smartinsure.dto.company;

import java.math.BigDecimal;

public record CompanyDashboardDto(
        long policyholders,
        long totalClaims,
        long pendingManualReview,
        long approvedClaims,
        long rejectedClaims,
        BigDecimal estimatedPayoutExposure
) {
}
