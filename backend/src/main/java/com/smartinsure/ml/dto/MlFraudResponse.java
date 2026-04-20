package com.smartinsure.ml.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MlFraudResponse(
        double fraudScore,
        String[] reasons
) {
}
