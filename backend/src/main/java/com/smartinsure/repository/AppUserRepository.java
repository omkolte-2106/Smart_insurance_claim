package com.smartinsure.repository;

import com.smartinsure.entity.AppUser;
import com.smartinsure.entity.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmailIgnoreCase(String email);

    long countByRole(UserRole role);
}
