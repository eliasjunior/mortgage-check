package com.mortgage.service;

import com.mortgage.exception.MaturityPeriodNotFoundException;
import com.mortgage.model.MortgageRate;
import com.mortgage.provider.MortgageRateProvider;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InterestRateService {

    private final MortgageRateProvider provider;

    public InterestRateService(MortgageRateProvider provider) {
        this.provider = provider;
    }

    public List<MortgageRate> findAllRates() {
        return provider.getRates();
    }

    public MortgageRate findByMaturityPeriod(int maturityPeriod) {
        return provider.getRates().stream()
                .filter(r -> r.maturityPeriod() == maturityPeriod)
                .findFirst()
                .orElseThrow(() -> new MaturityPeriodNotFoundException(maturityPeriod));
    }
}
