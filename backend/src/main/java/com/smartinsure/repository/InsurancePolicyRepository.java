package com.smartinsure.repository;

import com.smartinsure.entity.InsurancePolicy;
import com.smartinsure.entity.enums.PolicyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InsurancePolicyRepository extends JpaRepository<InsurancePolicy, Long> {
    Optional<InsurancePolicy> findByPolicyNumberIgnoreCase(String policyNumber);

    List<InsurancePolicy> findByCustomerId(Long customerId);

    List<InsurancePolicy> findByCompanyId(Long companyId);

    long countByCompanyIdAndStatus(Long companyId, PolicyStatus status);

    long countByCompanyId(Long companyId);
}
