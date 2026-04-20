package com.smartinsure.controller;

import com.smartinsure.dto.customer.AddVehicleRequest;
import com.smartinsure.entity.InsurancePolicy;
import com.smartinsure.entity.Vehicle;
import com.smartinsure.repository.InsurancePolicyRepository;
import com.smartinsure.service.CurrentUserService;
import com.smartinsure.service.CustomerVehicleService;
import com.smartinsure.service.DiscountService;
import com.smartinsure.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CurrentUserService currentUserService;
    private final CustomerVehicleService customerVehicleService;
    private final InsurancePolicyRepository insurancePolicyRepository;
    private final DiscountService discountService;
    private final ProfileService profileService;

    @GetMapping("/me")
    public Map<String, Object> me() {
        return profileService.buildUserSummary(currentUserService.currentUser().getId());
    }

    @PostMapping("/vehicles")
    public Vehicle addVehicle(@Valid @RequestBody AddVehicleRequest request) {
        return customerVehicleService.addVehicle(currentUserService.currentUser(), request);
    }

    @GetMapping("/vehicles")
    public List<Vehicle> vehicles() {
        return customerVehicleService.myVehicles(currentUserService.currentUser());
    }

    @GetMapping("/policies")
    public List<InsurancePolicy> policies() {
        var user = currentUserService.currentUser();
        return insurancePolicyRepository.findByCustomerId(user.getCustomerProfileId());
    }

    @GetMapping("/discount")
    public Map<String, Object> discount() {
        var user = currentUserService.currentUser();
        return discountService.latestForCustomer(user.getCustomerProfileId())
                .map(d -> Map.<String, Object>of(
                        "eligible", true,
                        "suggestedDiscountPercent", d.getSuggestedDiscountPercent(),
                        "percentileRank", d.getPercentileRank(),
                        "campaignCode", d.getCampaignCode()
                ))
                .orElse(Map.of("eligible", false));
    }
}
