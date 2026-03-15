package com.mortgage.controller;

import com.mortgage.model.MortgageRate;
import com.mortgage.service.InterestRateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class InterestRateController {

    private final InterestRateService interestRateService;

    public InterestRateController(InterestRateService interestRateService) {
        this.interestRateService = interestRateService;
    }

    @GetMapping("/interest-rates")
    public ResponseEntity<List<MortgageRate>> getInterestRates() {
        return ResponseEntity.ok(interestRateService.findAllRates());
    }
}
