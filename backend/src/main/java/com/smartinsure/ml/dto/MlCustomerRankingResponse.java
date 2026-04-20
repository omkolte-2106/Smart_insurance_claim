package com.smartinsure.ml.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MlCustomerRankingResponse(
        List<RankedCustomer> rankedCustomers
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RankedCustomer(
            String externalCustomerKey,
            double percentileRank,
            BigDecimal suggestedDiscountPercent,
            String rationale
    ) {
    }
}
