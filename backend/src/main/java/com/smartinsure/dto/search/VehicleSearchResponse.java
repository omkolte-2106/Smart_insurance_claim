package com.smartinsure.dto.search;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class VehicleSearchResponse {
    String registrationNumber;
    String customerName;
    String customerEmail;
    String insurerName;
    List<String> activePolicyNumbers;
}
