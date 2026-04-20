package com.smartinsure.ml.dto;

import java.util.List;

public record MlDamagePartsResponse(
        List<String> detectedParts,
        String severity,
        String modelVersion
) {
}
