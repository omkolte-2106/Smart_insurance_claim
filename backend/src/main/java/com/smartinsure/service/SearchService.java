package com.smartinsure.service;

import com.smartinsure.dto.search.VehicleSearchResponse;
import com.smartinsure.entity.InsurancePolicy;
import com.smartinsure.entity.Vehicle;
import com.smartinsure.entity.enums.PolicyStatus;
import com.smartinsure.entity.enums.UserRole;
import com.smartinsure.exception.ApiException;
import com.smartinsure.repository.InsurancePolicyRepository;
import com.smartinsure.repository.VehicleRepository;
import com.smartinsure.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final VehicleRepository vehicleRepository;
    private final InsurancePolicyRepository insurancePolicyRepository;

    @Transactional(readOnly = true)
    public VehicleSearchResponse searchByVehicleNumber(SecurityUser user, String registration) {
        if (user.getRole() != UserRole.ROLE_ADMIN && user.getRole() != UserRole.ROLE_COMPANY) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Search not permitted for this role");
        }
        Vehicle vehicle = vehicleRepository.findByRegistrationNumberIgnoreCase(registration)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Vehicle not found"));
        if (user.getRole() == UserRole.ROLE_COMPANY) {
            boolean linked = insurancePolicyRepository.findByCompanyId(user.getCompanyProfileId()).stream()
                    .anyMatch(p -> p.getVehicle().getId().equals(vehicle.getId()));
            if (!linked) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Vehicle not linked to your portfolio");
            }
        }
        List<String> policies = insurancePolicyRepository.findByCustomerId(vehicle.getCustomer().getId()).stream()
                .filter(p -> p.getStatus() == PolicyStatus.ACTIVE)
                .map(InsurancePolicy::getPolicyNumber)
                .collect(Collectors.toList());
        String insurer = insurancePolicyRepository.findByCustomerId(vehicle.getCustomer().getId()).stream()
                .filter(p -> p.getVehicle().getId().equals(vehicle.getId()))
                .map(p -> p.getCompany().getLegalName())
                .findFirst()
                .orElse("Unknown");
        return VehicleSearchResponse.builder()
                .registrationNumber(vehicle.getRegistrationNumber())
                .customerName(vehicle.getCustomer().getFullName())
                .customerEmail(vehicle.getCustomer().getUser().getEmail())
                .insurerName(insurer)
                .activePolicyNumbers(policies)
                .build();
    }
}
