package com.smartinsure.service;

import com.smartinsure.dto.customer.AddVehicleRequest;
import com.smartinsure.entity.CustomerProfile;
import com.smartinsure.entity.Vehicle;
import com.smartinsure.exception.ApiException;
import com.smartinsure.repository.CustomerProfileRepository;
import com.smartinsure.repository.VehicleRepository;
import com.smartinsure.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerVehicleService {

    private final CustomerProfileRepository customerProfileRepository;
    private final VehicleRepository vehicleRepository;

    @Transactional
    public Vehicle addVehicle(SecurityUser user, AddVehicleRequest req) {
        if (user.getCustomerProfileId() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Customer profile required");
        }
        CustomerProfile customer = customerProfileRepository.findById(user.getCustomerProfileId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Customer not found"));
        Vehicle vehicle = Vehicle.builder()
                .customer(customer)
                .registrationNumber(req.getRegistrationNumber().trim().toUpperCase())
                .make(req.getMake())
                .model(req.getModel())
                .yearOfManufacture(req.getYearOfManufacture())
                .build();
        return vehicleRepository.save(vehicle);
    }

    @Transactional(readOnly = true)
    public List<Vehicle> myVehicles(SecurityUser user) {
        if (user.getCustomerProfileId() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Customer profile required");
        }
        return vehicleRepository.findByCustomerId(user.getCustomerProfileId());
    }
}
