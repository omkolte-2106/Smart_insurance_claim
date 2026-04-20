package com.smartinsure.repository;

import com.smartinsure.entity.DiscountEligibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DiscountEligibilityRepository extends JpaRepository<DiscountEligibility, Long> {
    List<DiscountEligibility> findByCustomerId(Long customerId);

    Optional<DiscountEligibility> findTopByCustomerIdOrderByUpdatedAtDesc(Long customerId);

    void deleteByCustomer_Id(Long customerId);

    @Query("select distinct d from DiscountEligibility d join fetch d.customer c join c.policies p where p.company.id = :companyId")
    List<DiscountEligibility> findForCompany(@Param("companyId") Long companyId);

    @Query("select distinct d from DiscountEligibility d join fetch d.customer")
    List<DiscountEligibility> findAllWithCustomer();
}
