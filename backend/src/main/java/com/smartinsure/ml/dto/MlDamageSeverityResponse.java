package com.smartinsure.ml.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MlDamageSeverityResponse(
        double severityScore,
        String severityLabel,
        String modelVersion
) {
}
