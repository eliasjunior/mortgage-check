package com.mortgage.exception;

public class MaturityPeriodNotFoundException extends RuntimeException {

    public MaturityPeriodNotFoundException(int maturityPeriod) {
        super("No interest rate found for maturity period: " + maturityPeriod);
    }
}
