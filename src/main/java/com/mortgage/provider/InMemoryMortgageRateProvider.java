package com.mortgage.provider;

import com.mortgage.model.MortgageRate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Component
public class InMemoryMortgageRateProvider implements MortgageRateProvider {

    private final List<MortgageRate> rates;

    public InMemoryMortgageRateProvider() {
        Instant now = Instant.now();
        this.rates = List.of(
                new MortgageRate(10, new BigDecimal("3.50"), now),
                new MortgageRate(15, new BigDecimal("3.75"), now),
                new MortgageRate(20, new BigDecimal("4.00"), now),
                new MortgageRate(25, new BigDecimal("4.25"), now),
                new MortgageRate(30, new BigDecimal("4.50"), now)
        );
    }

    @Override
    public List<MortgageRate> getRates() {
        return rates;
    }
}
