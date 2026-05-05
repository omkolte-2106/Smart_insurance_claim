package com.smartinsure.ml.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MlPayoutEstimateResponse(
        BigDecimal recommendedPayout,
        BigDecimal minPayout,
        BigDecimal maxPayout,
        String currency,
        String rationale
) {
}
