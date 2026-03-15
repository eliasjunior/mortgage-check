package com.mortgage.service;

import com.mortgage.exception.MaturityPeriodNotFoundException;
import com.mortgage.model.MortgageCheckRequest;
import com.mortgage.model.MortgageCheckResponse;
import com.mortgage.model.MortgageRate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MortgageServiceTest {

    @Mock
    private InterestRateService interestRateService;

    @InjectMocks
    private MortgageService mortgageService;

    private static final MortgageRate RATE_30Y = new MortgageRate(30, 
        new BigDecimal("4.50"), Instant.now());

    @Test
    void feasible_whenLoanWithinIncomeAndHomeValueLimits() {
        when(interestRateService.findByMaturityPeriod(30)).thenReturn(RATE_30Y);
        MortgageCheckRequest request = new MortgageCheckRequest(
                new BigDecimal("100000"),
                30,
                new BigDecimal("300000"),
                new BigDecimal("350000")
        );

        MortgageCheckResponse response = mortgageService.check(request);

        assertThat(response.feasible()).isTrue();
        assertThat(response.monthlyCosts()).isPositive();
    }

    @Test
    void notFeasible_whenLoanExceedsFourTimesIncome() {
        when(interestRateService.findByMaturityPeriod(30)).thenReturn(RATE_30Y);
        MortgageCheckRequest request = new MortgageCheckRequest(
                new BigDecimal("50000"),
                30,
                new BigDecimal("250000"),  // exceeds 4x income (200k)
                new BigDecimal("350000")
        );

        MortgageCheckResponse response = mortgageService.check(request);

        assertThat(response.feasible()).isFalse();
        assertThat(response.monthlyCosts()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void notFeasible_whenLoanExceedsHomeValue() {
        when(interestRateService.findByMaturityPeriod(30)).thenReturn(RATE_30Y);
        MortgageCheckRequest request = new MortgageCheckRequest(
                new BigDecimal("200000"),
                30,
                new BigDecimal("400000"),  // exceeds homeValue (350k)
                new BigDecimal("350000")
        );

        MortgageCheckResponse response = mortgageService.check(request);

        assertThat(response.feasible()).isFalse();
        assertThat(response.monthlyCosts()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void notFeasible_whenLoanExceedsBothLimits() {
        when(interestRateService.findByMaturityPeriod(30)).thenReturn(RATE_30Y);
        MortgageCheckRequest request = new MortgageCheckRequest(
                new BigDecimal("50000"),
                30,
                new BigDecimal("600000"),  // exceeds both limits
                new BigDecimal("350000")
        );

        MortgageCheckResponse response = mortgageService.check(request);

        assertThat(response.feasible()).isFalse();
    }

    @Test
    void feasible_whenLoanEqualsExactlyFourTimesIncome() {
        when(interestRateService.findByMaturityPeriod(30)).thenReturn(RATE_30Y);
        MortgageCheckRequest request = new MortgageCheckRequest(
                new BigDecimal("100000"),
                30,
                new BigDecimal("400000"),  // exactly 4x income
                new BigDecimal("500000")
        );

        MortgageCheckResponse response = mortgageService.check(request);

        assertThat(response.feasible()).isTrue();
    }

    @Test
    void feasible_whenLoanEqualsExactlyHomeValue() {
        when(interestRateService.findByMaturityPeriod(30)).thenReturn(RATE_30Y);
        MortgageCheckRequest request = new MortgageCheckRequest(
                new BigDecimal("100000"),
                30,
                new BigDecimal("350000"),  // exactly home value
                new BigDecimal("350000")
        );

        MortgageCheckResponse response = mortgageService.check(request);

        assertThat(response.feasible()).isTrue();
    }

    @Test
    void throwsException_whenMaturityPeriodHasNoRate() {
        when(interestRateService.findByMaturityPeriod(99))
                .thenThrow(new MaturityPeriodNotFoundException(99));

        MortgageCheckRequest request = new MortgageCheckRequest(
                new BigDecimal("100000"),
                99,
                new BigDecimal("300000"),
                new BigDecimal("350000")
        );

        assertThatThrownBy(() -> mortgageService.check(request))
                .isInstanceOf(MaturityPeriodNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void monthlyCosts_areCorrectlyCalculated() {
        when(interestRateService.findByMaturityPeriod(30)).thenReturn(RATE_30Y);
        // 300k loan, 4.5% annual — expected: 300000 * 4.5 / 100 / 12 = 1125.00
        MortgageCheckRequest request = new MortgageCheckRequest(
                new BigDecimal("100000"),
                30,
                new BigDecimal("300000"),
                new BigDecimal("400000")
        );

        MortgageCheckResponse response = mortgageService.check(request);

        assertThat(response.monthlyCosts())
                .isEqualByComparingTo(new BigDecimal("1125.00"));
    }
}
