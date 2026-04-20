package com.smartinsure.controller;

import com.smartinsure.dto.auth.AuthResponse;
import com.smartinsure.dto.auth.LoginRequest;
import com.smartinsure.dto.auth.RegisterCompanyRequest;
import com.smartinsure.dto.auth.RegisterCustomerRequest;
import com.smartinsure.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register/customer")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerCustomer(@Valid @RequestBody RegisterCustomerRequest request) {
        return authService.registerCustomer(request);
    }

    @PostMapping("/register/company")
    @ResponseStatus(HttpStatus.CREATED)
    public void registerCompany(@Valid @RequestBody RegisterCompanyRequest request) {
        authService.registerCompany(request);
    }
}
