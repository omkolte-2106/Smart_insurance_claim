package com.smartinsure.dto.company;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreatePolicyRequest {
    @Email
    @NotBlank
    private String customerEmail;

    @NotBlank
    private String vehicleRegistration;

    @NotBlank
    private String policyNumber;

    @NotNull
    private BigDecimal sumInsured;

    @NotNull
    private BigDecimal annualPremium;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;
}
