package com.smartinsure.service;

import com.smartinsure.dto.company.BulkPolicyResponse;
import com.smartinsure.dto.company.CreatePolicyRequest;
import com.smartinsure.entity.CompanyProfile;
import com.smartinsure.entity.CustomerProfile;
import com.smartinsure.entity.InsurancePolicy;
import com.smartinsure.entity.Vehicle;
import com.smartinsure.entity.enums.PolicyStatus;
import com.smartinsure.entity.enums.UserRole;
import com.smartinsure.exception.ApiException;
import com.smartinsure.repository.CompanyProfileRepository;
import com.smartinsure.repository.CustomerProfileRepository;
import com.smartinsure.repository.InsurancePolicyRepository;
import com.smartinsure.repository.VehicleRepository;
import com.smartinsure.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyService {

    private final CompanyProfileRepository companyProfileRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final VehicleRepository vehicleRepository;
    private final InsurancePolicyRepository insurancePolicyRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final com.smartinsure.repository.AppUserRepository appUserRepository;

    @Transactional
    public InsurancePolicy createPolicyForCompany(SecurityUser user, CreatePolicyRequest req) {
        if (user.getRole() != UserRole.ROLE_COMPANY) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Company role required");
        }
        CompanyProfile company = companyProfileRepository.findById(user.getCompanyProfileId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Company not found"));
        
        return issueSinglePolicy(user, company, req);
    }

    private InsurancePolicy issueSinglePolicy(SecurityUser user, CompanyProfile company, CreatePolicyRequest req) {
        CustomerProfile customer = customerProfileRepository.findByUserEmailIgnoreCase(req.getCustomerEmail())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Customer email not registered: " + req.getCustomerEmail()));
        Vehicle vehicle = vehicleRepository
                .findByCustomerIdAndRegistrationIgnoreCase(customer.getId(), req.getVehicleRegistration())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Vehicle " + req.getVehicleRegistration() + " not found for customer " + req.getCustomerEmail()));
        insurancePolicyRepository.findByPolicyNumberIgnoreCase(req.getPolicyNumber()).ifPresent(p -> {
            throw new ApiException(HttpStatus.CONFLICT, "Policy number already exists: " + req.getPolicyNumber());
        });
        
        InsurancePolicy policy = InsurancePolicy.builder()
                .company(company)
                .customer(customer)
                .vehicle(vehicle)
                .policyNumber(req.getPolicyNumber())
                .sumInsured(req.getSumInsured())
                .annualPremium(req.getAnnualPremium())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .status(PolicyStatus.ACTIVE)
                .build();
        
        InsurancePolicy saved = insurancePolicyRepository.save(policy);
        
        auditService.record(appUserRepository.findById(user.getId()).orElse(null),
                "POLICY_ISSUED", "InsurancePolicy", saved.getPolicyNumber(), customer.getFullName());
        
        notificationService.notify(customer.getUser(),
                "New motor policy issued",
                "Policy " + saved.getPolicyNumber() + " is now active with " + company.getLegalName());
        
        return saved;
    }

    @Transactional
    public BulkPolicyResponse bulkIssuePolicies(SecurityUser user, MultipartFile file) {
        if (user.getRole() != UserRole.ROLE_COMPANY) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Company role required");
        }
        CompanyProfile company = companyProfileRepository.findById(user.getCompanyProfileId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Company not found"));

        BulkPolicyResponse response = new BulkPolicyResponse();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) { // Skip header
                    firstLine = false;
                    continue;
                }
                if (line.trim().isEmpty()) continue;
                
                String[] parts = line.split(",");
                // Basic validation: must have at least 3 parts for basic info
                if (parts.length < 3) {
                    response.setFailureCount(response.getFailureCount() + 1);
                    response.getErrors().add("Invalid format (missing columns): " + line);
                    continue;
                }

                try {
                    CreatePolicyRequest req = new CreatePolicyRequest();
                    req.setCustomerEmail(parts[0].trim());
                    req.setVehicleRegistration(parts[1].trim());
                    req.setPolicyNumber(parts.length > 2 ? parts[2].trim() : "POL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                    req.setSumInsured(parts.length > 3 ? new BigDecimal(parts[3].trim()) : BigDecimal.ZERO);
                    req.setAnnualPremium(parts.length > 4 ? new BigDecimal(parts[4].trim()) : BigDecimal.ZERO);
                    req.setStartDate(parts.length > 5 ? LocalDate.parse(parts[5].trim()) : LocalDate.now());
                    req.setEndDate(parts.length > 6 ? LocalDate.parse(parts[6].trim()) : LocalDate.now().plusYears(1));

                    // We wrap the individual call to handle exceptions per row
                    issueSinglePolicy(user, company, req);
                    response.setSuccessCount(response.getSuccessCount() + 1);
                } catch (Exception e) {
                    log.error("Failed to process bulk policy row: {}", line, e);
                    response.setFailureCount(response.getFailureCount() + 1);
                    response.getErrors().add("Row " + line + ": " + (e instanceof ApiException ? e.getMessage() : "Internal error"));
                }
            }
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to parse CSV: " + e.getMessage());
        }
        return response;
    }

    @Transactional(readOnly = true)
    public String generatePolicyTemplateCsv(boolean prefill) {
        StringBuilder csv = new StringBuilder();
        csv.append("customerEmail,vehicleRegistration,policyNumber,sumInsured,annualPremium,startDate,endDate\n");
        
        if (prefill) {
            List<Vehicle> uninsured = vehicleRepository.findUninsuredVehicles();
            for (Vehicle v : uninsured) {
                csv.append(v.getCustomer().getUser().getEmail()).append(",");
                csv.append(v.getRegistrationNumber()).append(",");
                csv.append("POL-").append(UUID.randomUUID().toString().substring(0, 8).toUpperCase()).append(",");
                csv.append("0,0,") // sumInsured, annualPremium
                   .append(LocalDate.now()).append(",")
                   .append(LocalDate.now().plusYears(1)).append("\n");
            }
        } else {
            // Add one sample row
            csv.append("example@customer.com,KA01AB1234,POL-SAMPLE-123,500000,15000,")
               .append(LocalDate.now()).append(",")
               .append(LocalDate.now().plusYears(1)).append("\n");
        }
        
        return csv.toString();
    }
}
