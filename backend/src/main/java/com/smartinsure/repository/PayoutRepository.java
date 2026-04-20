package com.smartinsure.repository;

import com.smartinsure.entity.Payout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PayoutRepository extends JpaRepository<Payout, Long> {
    Optional<Payout> findByClaimId(Long claimId);
}
