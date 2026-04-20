package com.smartinsure.ml.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MlDocumentVerificationResponse(
        double clarityScore,
        double validityScore,
        double fraudSuspicion,
        String notes
) {
}
