package com.smartinsure.repository;

import com.smartinsure.entity.CompanyProfile;
import com.smartinsure.entity.enums.CompanyApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyProfileRepository extends JpaRepository<CompanyProfile, Long> {
    Optional<CompanyProfile> findByUserId(Long userId);

    long countByApprovalStatus(CompanyApprovalStatus status);

    Page<CompanyProfile> findByApprovalStatus(CompanyApprovalStatus status, Pageable pageable);
}
