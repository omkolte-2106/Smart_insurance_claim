package com.smartinsure.dto.company;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkPolicyResponse {
    private int successCount;
    private int failureCount;
    @Builder.Default
    private List<String> errors = new ArrayList<>();
}
