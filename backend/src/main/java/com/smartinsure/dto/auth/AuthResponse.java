package com.smartinsure.dto.auth;

import com.smartinsure.entity.enums.UserRole;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthResponse {
    String token;
    UserRole role;
    String email;
    Long companyProfileId;
    Long customerProfileId;
}
