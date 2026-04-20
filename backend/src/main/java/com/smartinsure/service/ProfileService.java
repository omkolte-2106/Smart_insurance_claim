package com.smartinsure.service;

import com.smartinsure.entity.AppUser;
import com.smartinsure.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final AppUserRepository appUserRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> buildUserSummary(Long userId) {
        AppUser user = appUserRepository.findById(userId).orElseThrow();
        Map<String, Object> map = new HashMap<>();
        map.put("email", user.getEmail());
        map.put("role", user.getRole());
        if (user.getCustomerProfile() != null) {
            map.put("customerProfileId", user.getCustomerProfile().getId());
            map.put("fullName", user.getCustomerProfile().getFullName());
        }
        if (user.getCompanyProfile() != null) {
            map.put("companyProfileId", user.getCompanyProfile().getId());
            map.put("companyName", user.getCompanyProfile().getLegalName());
            map.put("companyApproval", user.getCompanyProfile().getApprovalStatus());
        }
        return map;
    }
}
