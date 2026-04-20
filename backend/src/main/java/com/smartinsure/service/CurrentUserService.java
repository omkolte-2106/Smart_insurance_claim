package com.smartinsure.service;

import com.smartinsure.entity.AppUser;
import com.smartinsure.exception.ApiException;
import com.smartinsure.repository.AppUserRepository;
import com.smartinsure.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final AppUserRepository appUserRepository;

    public SecurityUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof SecurityUser su)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return su;
    }

    @Transactional(readOnly = true)
    public AppUser currentAppUser() {
        SecurityUser su = currentUser();
        return appUserRepository.findById(su.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
