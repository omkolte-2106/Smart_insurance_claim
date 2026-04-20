package com.smartinsure.service;

import com.smartinsure.entity.CustomerProfile;
import com.smartinsure.entity.DiscountEligibility;
import com.smartinsure.entity.enums.UserRole;
import com.smartinsure.exception.ApiException;
import com.smartinsure.ml.MlServiceClient;
import com.smartinsure.ml.dto.MlCustomerRankingResponse;
import com.smartinsure.repository.ClaimRepository;
import com.smartinsure.repository.CustomerProfileRepository;
import com.smartinsure.repository.DiscountEligibilityRepository;
import com.smartinsure.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * ML-ready discount analytics. Falls back to deterministic heuristics when the ML service
 * returns no ranking payload.
 */
@Service
@RequiredArgsConstructor
public class DiscountService {

    private final CustomerProfileRepository customerProfileRepository;
    private final ClaimRepository claimRepository;
    private final DiscountEligibilityRepository discountEligibilityRepository;
    private final MlServiceClient mlServiceClient;
    private final AuditService auditService;
    private final com.smartinsure.repository.AppUserRepository appUserRepository;

    @Transactional
    public List<DiscountEligibility> recomputeTopCustomers(SecurityUser user, double topFraction) {
        if (user.getRole() != UserRole.ROLE_ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin only");
        }
        if (topFraction <= 0 || topFraction > 0.5) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "topFraction must be between 0 and 0.5");
        }
        List<CustomerProfile> customers = customerProfileRepository.findAll();
        Map<String, Object> payload = Map.of(
                "topFraction", topFraction,
                "customers", customers.stream().map(c -> Map.of(
                        "id", c.getId(),
                        "loyaltyScore", c.getLoyaltyScore(),
                        "claims", claimRepository.findByCustomerId(c.getId(), org.springframework.data.domain.Pageable.unpaged()).getTotalElements()
                )).toList()
        );
        MlCustomerRankingResponse mlResponse = mlServiceClient.rankCustomers(payload);
        discountEligibilityRepository.deleteAll();

        if (mlResponse.rankedCustomers() != null && !mlResponse.rankedCustomers().isEmpty()) {
            // Placeholder path when ML service returns structured ranking keyed by external id = customer id string
            for (var row : mlResponse.rankedCustomers()) {
                long id = Long.parseLong(row.externalCustomerKey());
                CustomerProfile cp = customerProfileRepository.findById(id).orElse(null);
                if (cp == null) {
                    continue;
                }
                DiscountEligibility de = DiscountEligibility.builder()
                        .customer(cp)
                        .percentileRank(row.percentileRank())
                        .suggestedDiscountPercent(row.suggestedDiscountPercent())
                        .rationaleJson("{\"source\":\"ml\"}")
                        .campaignCode("AUTO_TOP_TIER")
                        .build();
                discountEligibilityRepository.save(de);
            }
        } else {
            List<ScoredCustomer> scored = customers.stream()
                    .map(c -> {
                        long claims = claimRepository.findByCustomerId(c.getId(),
                                org.springframework.data.domain.Pageable.unpaged()).getTotalElements();
                        double score = c.getLoyaltyScore() - claims * 4;
                        return new ScoredCustomer(c, score);
                    })
                    .sorted(Comparator.comparingDouble(ScoredCustomer::score).reversed())
                    .toList();
            int take = Math.max(1, (int) Math.ceil(scored.size() * topFraction));
            for (int i = 0; i < take; i++) {
                CustomerProfile cp = scored.get(i).profile();
                double percentile = 100.0 - (100.0 * i / Math.max(1, scored.size()));
                BigDecimal discount = BigDecimal.valueOf(8 + (i % 5)).setScale(2, RoundingMode.HALF_UP);
                DiscountEligibility de = DiscountEligibility.builder()
                        .customer(cp)
                        .percentileRank(percentile)
                        .suggestedDiscountPercent(discount)
                        .rationaleJson("{\"source\":\"heuristic\",\"claims\":"
                                + claimRepository.findByCustomerId(cp.getId(),
                                org.springframework.data.domain.Pageable.unpaged()).getTotalElements() + "}")
                        .campaignCode("LOYALTY_FALLBACK")
                        .build();
                discountEligibilityRepository.save(de);
            }
        }
        auditService.record(appUserRepository.findById(user.getId()).orElse(null),
                "DISCOUNT_RECOMPUTE", "DiscountEligibility", "ALL", "topFraction=" + topFraction);
        return discountEligibilityRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<DiscountEligibility> listAll(SecurityUser user) {
        if (user.getRole() != UserRole.ROLE_ADMIN && user.getRole() != UserRole.ROLE_COMPANY) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed");
        }
        if (user.getRole() == UserRole.ROLE_COMPANY) {
            return discountEligibilityRepository.findForCompany(user.getCompanyProfileId());
        }
        return discountEligibilityRepository.findAllWithCustomer();
    }

    @Transactional(readOnly = true)
    public Optional<DiscountEligibility> latestForCustomer(Long customerProfileId) {
        return discountEligibilityRepository.findTopByCustomerIdOrderByUpdatedAtDesc(customerProfileId);
    }

    private record ScoredCustomer(CustomerProfile profile, double score) {
    }
}
