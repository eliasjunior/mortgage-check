package com.mortgage.provider;

import com.mortgage.model.MortgageRate;

import java.util.List;

public interface MortgageRateProvider {

    List<MortgageRate> getRates();
}
