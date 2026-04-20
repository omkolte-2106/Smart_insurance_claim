package com.smartinsure.dto.customer;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddVehicleRequest {
    @NotBlank
    private String registrationNumber;
    private String make;
    private String model;
    private Integer yearOfManufacture;
}
