package com.smartinsure.repository;

import com.smartinsure.entity.VerificationResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VerificationResultRepository extends JpaRepository<VerificationResult, Long> {
    List<VerificationResult> findByClaimIdOrderByCreatedAtDesc(Long claimId);
}
