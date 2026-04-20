package com.smartinsure.repository;

import com.smartinsure.entity.Claim;
import com.smartinsure.entity.enums.ClaimStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClaimRepository extends JpaRepository<Claim, Long> {
    Optional<Claim> findByClaimPublicIdIgnoreCase(String claimPublicId);

    Page<Claim> findByCompanyId(Long companyId, Pageable pageable);

    Page<Claim> findByCustomerId(Long customerId, Pageable pageable);

    Page<Claim> findByStatus(ClaimStatus status, Pageable pageable);

    long countByStatus(ClaimStatus status);

    long countByCompanyId(Long companyId);

    long countByFraudFlaggedTrue();

    Page<Claim> findByFraudFlaggedTrue(Pageable pageable);

    @Query("select c from Claim c where c.company.id = :companyId and (:status is null or c.status = :status)")
    Page<Claim> findByCompanyAndOptionalStatus(@Param("companyId") Long companyId,
                                               @Param("status") ClaimStatus status,
                                               Pageable pageable);
}
