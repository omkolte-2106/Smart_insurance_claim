package com.smartinsure.service;

import com.smartinsure.dto.auth.AuthResponse;
import com.smartinsure.dto.auth.LoginRequest;
import com.smartinsure.dto.auth.RegisterCompanyRequest;
import com.smartinsure.dto.auth.RegisterCustomerRequest;
import com.smartinsure.entity.AppUser;
import com.smartinsure.entity.CompanyProfile;
import com.smartinsure.entity.CustomerProfile;
import com.smartinsure.entity.enums.CompanyApprovalStatus;
import com.smartinsure.entity.enums.UserRole;
import com.smartinsure.exception.ApiException;
import com.smartinsure.repository.AppUserRepository;
import com.smartinsure.security.JwtTokenProvider;
import com.smartinsure.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse registerCustomer(RegisterCustomerRequest req) {
        appUserRepository.findByEmailIgnoreCase(req.getEmail()).ifPresent(u -> {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        });
        AppUser user = AppUser.builder()
                .email(req.getEmail().toLowerCase())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role(UserRole.ROLE_CUSTOMER)
                .enabled(true)
                .banned(false)
                .build();
        CustomerProfile profile = CustomerProfile.builder()
                .user(user)
                .fullName(req.getFullName())
                .phone(req.getPhone())
                .address(req.getAddress())
                .loyaltyScore(60)
                .banned(false)
                .build();
        user.setCustomerProfile(profile);
        profile.setUser(user);
        appUserRepository.save(user);
        SecurityUser principal = new SecurityUser(user);
        return AuthResponse.builder()
                .token(jwtTokenProvider.generateToken(principal))
                .role(user.getRole())
                .email(user.getEmail())
                .customerProfileId(profile.getId())
                .build();
    }

    @Transactional
    public void registerCompany(RegisterCompanyRequest req) {
        appUserRepository.findByEmailIgnoreCase(req.getEmail()).ifPresent(u -> {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        });
        AppUser user = AppUser.builder()
                .email(req.getEmail().toLowerCase())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role(UserRole.ROLE_COMPANY)
                .enabled(true)
                .banned(false)
                .build();
        CompanyProfile company = CompanyProfile.builder()
                .user(user)
                .legalName(req.getLegalName())
                .gstNumber(req.getGstNumber())
                .approvalStatus(CompanyApprovalStatus.PENDING)
                .contactEmail(req.getContactEmail())
                .contactPhone(req.getContactPhone())
                .banned(false)
                .build();
        user.setCompanyProfile(company);
        company.setUser(user);
        appUserRepository.save(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        AppUser user = appUserRepository.findByEmailIgnoreCase(req.getEmail())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        if (user.isBanned()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Account is banned");
        }
        if (user.getRole() == UserRole.ROLE_COMPANY) {
            CompanyProfile cp = user.getCompanyProfile();
            if (cp == null) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Company profile missing");
            }
            if (cp.getApprovalStatus() != CompanyApprovalStatus.APPROVED) {
                throw new ApiException(HttpStatus.FORBIDDEN,
                        "Company registration is pending admin approval. You cannot sign in yet.");
            }
            if (cp.isBanned()) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Company is banned");
            }
        }
        if (user.getRole() == UserRole.ROLE_CUSTOMER && user.getCustomerProfile() != null
                && user.getCustomerProfile().isBanned()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Customer account is banned");
        }
        SecurityUser principal = new SecurityUser(user);
        return AuthResponse.builder()
                .token(jwtTokenProvider.generateToken(principal))
                .role(user.getRole())
                .email(user.getEmail())
                .companyProfileId(user.getCompanyProfile() != null ? user.getCompanyProfile().getId() : null)
                .customerProfileId(user.getCustomerProfile() != null ? user.getCustomerProfile().getId() : null)
                .build();
    }
}
