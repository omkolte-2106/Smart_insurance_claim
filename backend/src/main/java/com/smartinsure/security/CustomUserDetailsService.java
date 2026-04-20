package com.smartinsure.security;

import com.smartinsure.entity.AppUser;
import com.smartinsure.entity.CompanyProfile;
import com.smartinsure.entity.enums.CompanyApprovalStatus;
import com.smartinsure.entity.enums.UserRole;
import com.smartinsure.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = appUserRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (user.getRole() == UserRole.ROLE_COMPANY) {
            CompanyProfile company = user.getCompanyProfile();
            if (company == null) {
                throw new UsernameNotFoundException("Company profile missing");
            }
            if (company.getApprovalStatus() != CompanyApprovalStatus.APPROVED) {
                throw new UsernameNotFoundException("Company registration is not approved yet");
            }
            if (company.isBanned()) {
                throw new UsernameNotFoundException("Company is banned");
            }
        }

        if (user.getRole() == UserRole.ROLE_CUSTOMER && user.getCustomerProfile() != null
                && user.getCustomerProfile().isBanned()) {
            throw new UsernameNotFoundException("Customer account is banned");
        }

        return new SecurityUser(user);
    }
}
