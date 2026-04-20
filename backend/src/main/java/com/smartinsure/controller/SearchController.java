package com.smartinsure.controller;

import com.smartinsure.dto.search.VehicleSearchResponse;
import com.smartinsure.service.CurrentUserService;
import com.smartinsure.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final CurrentUserService currentUserService;

    @GetMapping("/vehicle")
    public VehicleSearchResponse searchVehicle(@RequestParam("registration") String registration) {
        return searchService.searchByVehicleNumber(currentUserService.currentUser(), registration);
    }
}
