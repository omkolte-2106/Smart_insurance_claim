package com.smartinsure.dto.lookup;

import lombok.Data;

@Data
public class VehicleLookupResponse {
    private String name;
    private String contactNumber;
    private String email;
    private String address;
    private String policyNumber;
    private String registrationNumber;
    private String make;
    private String model;
    private String variant;
    private String chassisNumber;
    private String engineNumber;
    private String fuelType;
    private Integer yearOfManufacture;
}
