package com.smartinsure.repository;

import com.smartinsure.entity.ClaimRemark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClaimRemarkRepository extends JpaRepository<ClaimRemark, Long> {
    List<ClaimRemark> findByClaimIdOrderByCreatedAtAsc(Long claimId);
}
