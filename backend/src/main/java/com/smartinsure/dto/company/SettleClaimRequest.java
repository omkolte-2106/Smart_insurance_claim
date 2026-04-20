package com.smartinsure.dto.company;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SettleClaimRequest {
    @NotNull
    private BigDecimal finalAmount;
}
