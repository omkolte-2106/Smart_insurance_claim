package com.smartinsure.controller;

import com.smartinsure.dto.lookup.VehicleLookupResponse;
import com.smartinsure.service.VehicleLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lookup")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Usually handled globally, but good to add if needed
public class VehicleLookupController {

    private final VehicleLookupService vehicleLookupService;

    @GetMapping("/vehicle")
    public ResponseEntity<VehicleLookupResponse> lookupVehicle(@RequestParam String regNumber) {
        VehicleLookupResponse response = vehicleLookupService.lookupByRegistrationNumber(regNumber);
        if (response != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
