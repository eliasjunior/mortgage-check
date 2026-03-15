package com.mortgage.provider;

import com.mortgage.model.MortgageRate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryMortgageRateProviderTest {

    private final MortgageRateProvider provider = new InMemoryMortgageRateProvider();

    @Test
    void getRates_returnsNonEmptyList() {
        assertThat(provider.getRates()).isNotEmpty();
    }

    @Test
    void getRates_allHavePositiveInterestRate() {
        provider.getRates().forEach(rate ->
                assertThat(rate.interestRate()).isPositive()
        );
    }

    @Test
    void getRates_allHaveLastUpdateSet() {
        provider.getRates().forEach(rate ->
                assertThat(rate.lastUpdate()).isNotNull()
        );
    }

    @Test
    void getRates_allHavePositiveMaturityPeriod() {
        provider.getRates().forEach(rate ->
                assertThat(rate.maturityPeriod()).isPositive()
        );
    }

    @Test
    void getRates_returnsImmutableList() {
        List<MortgageRate> rates = provider.getRates();
        assertThat(rates).isUnmodifiable();
    }
}
