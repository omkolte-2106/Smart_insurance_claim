package com.smartinsure.repository;

import com.smartinsure.entity.FraudFlag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FraudFlagRepository extends JpaRepository<FraudFlag, Long> {
    Page<FraudFlag> findByActiveTrue(Pageable pageable);
}
