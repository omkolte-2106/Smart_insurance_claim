package com.smartinsure.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartinsure.dto.lookup.VehicleLookupResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class VehicleLookupService {

    private final ObjectMapper objectMapper;
    
    // In-memory cache for O(1) lookups by registration number
    private final Map<String, VehicleLookupResponse> vehicleDataMap = new HashMap<>();

    public VehicleLookupService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("indian_motor_insurance_data.json");
            if (!resource.exists()) {
                log.warn("indian_motor_insurance_data.json not found in resources. Vehicle lookup will be empty.");
                return;
            }
            
            try (InputStream inputStream = resource.getInputStream()) {
                List<JsonNode> records = objectMapper.readValue(inputStream, new TypeReference<List<JsonNode>>() {});
                
                for (JsonNode record : records) {
                    JsonNode insured = record.path("insured_details");
                    JsonNode vehicle = record.path("vehicle_details");
                    
                    VehicleLookupResponse response = new VehicleLookupResponse();
                    
                    // Set insured details
                    response.setName(insured.path("name").asText(null));
                    response.setContactNumber(insured.path("contact_number").asText(null));
                    response.setEmail(insured.path("email").asText(null));
                    response.setAddress(insured.path("address").asText(null));
                    
                    // Set vehicle details
                    response.setPolicyNumber(vehicle.path("policy_number").asText(null));
                    String regNum = vehicle.path("registration_number").asText(null);
                    response.setRegistrationNumber(regNum);
                    response.setMake(vehicle.path("make").asText(null));
                    response.setModel(vehicle.path("model").asText(null));
                    response.setVariant(vehicle.path("variant").asText(null));
                    response.setChassisNumber(vehicle.path("chassis_number").asText(null));
                    response.setEngineNumber(vehicle.path("engine_number").asText(null));
                    response.setFuelType(vehicle.path("fuel_type").asText(null));
                    
                    JsonNode yearNode = vehicle.path("year_of_manufacture");
                    if (!yearNode.isMissingNode() && !yearNode.isNull()) {
                        response.setYearOfManufacture(yearNode.asInt());
                    }
                    
                    if (regNum != null && !regNum.isEmpty()) {
                        // Normalize registration number (remove spaces/hyphens and upper case) for better matching
                        String normalizedRegNum = regNum.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
                        vehicleDataMap.put(normalizedRegNum, response);
                    }
                }
                log.info("Successfully loaded {} vehicle records into lookup cache", vehicleDataMap.size());
            }
        } catch (Exception e) {
            log.error("Failed to load vehicle lookup data", e);
        }
    }

    public VehicleLookupResponse lookupByRegistrationNumber(String regNumber) {
        if (regNumber == null || regNumber.trim().isEmpty()) {
            return null;
        }
        String normalizedRegNum = regNumber.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        return vehicleDataMap.get(normalizedRegNum);
    }
}
