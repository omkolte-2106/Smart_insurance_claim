package com.smartinsure.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterCompanyRequest {
    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Size(min = 3, max = 80)
    private String password;

    @NotBlank
    private String legalName;

    private String gstNumber;
    private String contactEmail;
    private String contactPhone;

    @NotBlank
    private String address;
}
