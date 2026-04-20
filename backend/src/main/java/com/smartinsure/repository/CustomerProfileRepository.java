package com.smartinsure.repository;

import com.smartinsure.entity.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {
    Optional<CustomerProfile> findByUserEmailIgnoreCase(String email);

    Optional<CustomerProfile> findByUserId(Long userId);
}
