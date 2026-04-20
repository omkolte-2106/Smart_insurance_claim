package com.smartinsure.dto.claim;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateClaimRequest {
    @NotNull
    private Long policyId;
    @NotBlank
    private String incidentDescription;
    private String incidentLocation;
}
