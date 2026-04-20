package com.smartinsure.util;

import com.smartinsure.repository.ClaimRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class ClaimIdGenerator {

    private final ClaimRepository claimRepository;

    /**
     * Generates a unique public claim id such as CLM-2026-458291.
     */
    public String next() {
        int year = Year.now().getValue();
        for (int i = 0; i < 12; i++) {
            int suffix = ThreadLocalRandom.current().nextInt(100_000, 999_999);
            String candidate = "CLM-" + year + "-" + suffix;
            if (claimRepository.findByClaimPublicIdIgnoreCase(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to allocate claim id");
    }
}
