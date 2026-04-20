package com.smartinsure.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterCustomerRequest {
    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Size(min = 8, max = 80)
    private String password;

    @NotBlank
    private String fullName;

    private String phone;
    private String address;
}
