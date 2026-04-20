package com.smartinsure.config;

import com.smartinsure.entity.*;
import com.smartinsure.entity.enums.*;
import com.smartinsure.repository.AppUserRepository;
import com.smartinsure.repository.InsurancePolicyRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Seeds realistic demo data for local development. Disabled when {@code dev} profile is not active.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final InsurancePolicyRepository insurancePolicyRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (appUserRepository.findByEmailIgnoreCase("admin@smartinsure.com").isPresent()) {
            log.info("Seed data already present, skipping.");
            return;
        }

        AppUser admin = AppUser.builder()
                .email("admin@smartinsure.com")
                .passwordHash(passwordEncoder.encode("Admin@123"))
                .role(UserRole.ROLE_ADMIN)
                .enabled(true)
                .banned(false)
                .build();
        appUserRepository.save(admin);

        AppUser digitUser = AppUser.builder()
                .email("digit@insurer.com")
                .passwordHash(passwordEncoder.encode("Company@123"))
                .role(UserRole.ROLE_COMPANY)
                .enabled(true)
                .banned(false)
                .build();
        CompanyProfile digit = CompanyProfile.builder()
                .user(digitUser)
                .legalName("Digit Motor Insurance Ltd.")
                .gstNumber("29AAACD1234F1Z5")
                .approvalStatus(CompanyApprovalStatus.APPROVED)
                .contactEmail("support@digitinsure.demo")
                .contactPhone("18001030909")
                .banned(false)
                .build();
        digitUser.setCompanyProfile(digit);
        digit.setUser(digitUser);
        appUserRepository.save(digitUser);

        AppUser ackoUser = AppUser.builder()
                .email("acko@insurer.com")
                .passwordHash(passwordEncoder.encode("Company@123"))
                .role(UserRole.ROLE_COMPANY)
                .enabled(true)
                .banned(false)
                .build();
        CompanyProfile acko = CompanyProfile.builder()
                .user(ackoUser)
                .legalName("Acko General Insurance")
                .gstNumber("27AAACA1234A1Z1")
                .approvalStatus(CompanyApprovalStatus.PENDING)
                .contactEmail("hello@acko.demo")
                .contactPhone("18003123456")
                .banned(false)
                .build();
        ackoUser.setCompanyProfile(acko);
        acko.setUser(ackoUser);
        appUserRepository.save(ackoUser);

        AppUser raviUser = AppUser.builder()
                .email("ravi@customer.com")
                .passwordHash(passwordEncoder.encode("Customer@123"))
                .role(UserRole.ROLE_CUSTOMER)
                .enabled(true)
                .banned(false)
                .build();
        CustomerProfile ravi = CustomerProfile.builder()
                .user(raviUser)
                .fullName("Ravi Sharma")
                .phone("9876543210")
                .address("Indiranagar, Bengaluru")
                .loyaltyScore(82)
                .banned(false)
                .build();
        raviUser.setCustomerProfile(ravi);
        ravi.setUser(raviUser);
        Vehicle raviCar = Vehicle.builder()
                .customer(ravi)
                .registrationNumber("KA03MN4455")
                .make("Hyundai")
                .model("Creta")
                .yearOfManufacture(2022)
                .build();
        ravi.getVehicles().add(raviCar);
        raviCar.setCustomer(ravi);
        appUserRepository.save(raviUser);
        entityManager.flush();

        InsurancePolicy policy = InsurancePolicy.builder()
                .company(digit)
                .customer(ravi)
                .vehicle(raviCar)
                .policyNumber("POL-DIGIT-900331")
                .sumInsured(new BigDecimal("950000"))
                .annualPremium(new BigDecimal("14500"))
                .startDate(LocalDate.now().minusMonths(3))
                .endDate(LocalDate.now().plusMonths(9))
                .status(PolicyStatus.ACTIVE)
                .build();
        insurancePolicyRepository.save(policy);

        AppUser priyaUser = AppUser.builder()
                .email("priya@customer.com")
                .passwordHash(passwordEncoder.encode("Customer@123"))
                .role(UserRole.ROLE_CUSTOMER)
                .enabled(true)
                .banned(false)
                .build();
        CustomerProfile priya = CustomerProfile.builder()
                .user(priyaUser)
                .fullName("Priya Nair")
                .phone("9123456780")
                .address("Koramangala, Bengaluru")
                .loyaltyScore(74)
                .banned(false)
                .build();
        priyaUser.setCustomerProfile(priya);
        priya.setUser(priyaUser);
        Vehicle priyaCar = Vehicle.builder()
                .customer(priya)
                .registrationNumber("KL07PQ8899")
                .make("Maruti")
                .model("Baleno")
                .yearOfManufacture(2021)
                .build();
        priya.getVehicles().add(priyaCar);
        priyaCar.setCustomer(priya);
        appUserRepository.save(priyaUser);

        log.info("SmartInsure demo users created (profile=dev).");
    }
}
