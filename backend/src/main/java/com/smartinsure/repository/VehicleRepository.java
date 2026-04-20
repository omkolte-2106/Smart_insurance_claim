package com.smartinsure.repository;

import com.smartinsure.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByCustomerId(Long customerId);

    @Query("select v from Vehicle v join v.customer c join c.user u where upper(v.registrationNumber) = upper(:reg)")
    Optional<Vehicle> findByRegistrationNumberIgnoreCase(@Param("reg") String registrationNumber);

    @Query("select v from Vehicle v join v.customer c where c.id = :customerId and upper(v.registrationNumber) = upper(:reg)")
    Optional<Vehicle> findByCustomerIdAndRegistrationIgnoreCase(@Param("customerId") Long customerId,
                                                                @Param("reg") String registrationNumber);

    @Query("SELECT v FROM Vehicle v JOIN FETCH v.customer c JOIN FETCH c.user u WHERE NOT EXISTS (SELECT p FROM InsurancePolicy p WHERE p.vehicle = v AND p.status = 'ACTIVE')")
    List<Vehicle> findUninsuredVehicles();
}
