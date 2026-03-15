package com.mortgage.service;

import com.mortgage.exception.MaturityPeriodNotFoundException;
import com.mortgage.model.MortgageRate;
import com.mortgage.provider.MortgageRateProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterestRateServiceTest {

    @Mock
    private MortgageRateProvider provider;

    @InjectMocks
    private InterestRateService service;

    private static final List<MortgageRate> SAMPLE_RATES = List.of(
            new MortgageRate(10, new BigDecimal("3.50"), Instant.now()),
            new MortgageRate(30, new BigDecimal("4.50"), Instant.now())
    );

    @Test
    void findAllRates_delegatesToProvider() {
        when(provider.getRates()).thenReturn(SAMPLE_RATES);

        List<MortgageRate> rates = service.findAllRates();

        assertThat(rates).isEqualTo(SAMPLE_RATES);
    }

    @Test
    void findByMaturityPeriod_returnsCorrectRate() {
        when(provider.getRates()).thenReturn(SAMPLE_RATES);

        MortgageRate rate = service.findByMaturityPeriod(30);

        assertThat(rate.maturityPeriod()).isEqualTo(30);
    }

    @Test
    void findByMaturityPeriod_throwsWhenNotFound() {
        when(provider.getRates()).thenReturn(SAMPLE_RATES);

        assertThatThrownBy(() -> service.findByMaturityPeriod(99))
                .isInstanceOf(MaturityPeriodNotFoundException.class)
                .hasMessageContaining("99");
    }
}
