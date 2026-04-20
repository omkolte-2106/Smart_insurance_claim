package com.smartinsure.controller;

import com.smartinsure.entity.DiscountEligibility;
import com.smartinsure.service.CurrentUserService;
import com.smartinsure.service.DiscountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/discounts")
@RequiredArgsConstructor
public class DiscountController {

    private final DiscountService discountService;
    private final CurrentUserService currentUserService;

    @GetMapping("/analytics")
    public List<DiscountEligibility> analytics() {
        return discountService.listAll(currentUserService.currentUser());
    }
}
