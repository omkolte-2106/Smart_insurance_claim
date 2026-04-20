package com.smartinsure.dto.claim;

import com.smartinsure.entity.enums.ClaimStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CompanyClaimActionRequest {
    @NotNull
    private ClaimStatus targetStatus;
    private String remarks;
}
