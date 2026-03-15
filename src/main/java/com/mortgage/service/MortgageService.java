package com.mortgage.service;

import com.mortgage.model.MortgageCheckRequest;
import com.mortgage.model.MortgageCheckResponse;
import com.mortgage.model.MortgageRate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class MortgageService {

    private static final BigDecimal MAX_INCOME_MULTIPLIER = new BigDecimal("4");
    private static final BigDecimal MONTHS_IN_YEAR = BigDecimal.valueOf(12);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final InterestRateService interestRateService;

    public MortgageService(InterestRateService interestRateService) {
        this.interestRateService = interestRateService;
    }

    public MortgageCheckResponse check(MortgageCheckRequest request) {
        MortgageRate rate = interestRateService.findByMaturityPeriod(request.maturityPeriod());

        boolean feasible = isFeasible(request);
        BigDecimal monthlyCosts = feasible
                ? calculateMonthlyCosts(request.loanValue(), rate.interestRate())
                : BigDecimal.ZERO;

        return new MortgageCheckResponse(feasible, monthlyCosts);
    }

    private boolean isFeasible(MortgageCheckRequest request) {
        BigDecimal maxLoanByIncome = request.income().multiply(MAX_INCOME_MULTIPLIER);
        boolean withinIncomeLimit = request.loanValue().compareTo(maxLoanByIncome) <= 0;
        boolean withinHomeValue = request.loanValue().compareTo(request.homeValue()) <= 0;
        return withinIncomeLimit && withinHomeValue;
    }

    private BigDecimal calculateMonthlyCosts(BigDecimal loanValue, BigDecimal annualRatePercent) {
        return loanValue
                .multiply(annualRatePercent)
                .divide(HUNDRED.multiply(MONTHS_IN_YEAR), 2, RoundingMode.HALF_UP);
    }
}
