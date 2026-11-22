package com.pricewatch.budget;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BudgetViewTest {
    @ParameterizedTest(name = "limit {0}, subtotal {1}, progress {2}%")
    @CsvSource(value = {"100, 25, 25, 75", "NONE, 25, 0, NONE", "100, 0, 0, 100"}, nullValues = "NONE")
    void eachZeroProgressConditionCanIndependentlyChangeTheResult(BigDecimal limit, BigDecimal subtotal,
                                                                  int progress, BigDecimal balance) {
        BudgetState state = limit == null ? BudgetState.NOT_SET : BudgetState.WITHIN_LIMIT;
        var view = new BudgetView(BudgetPeriod.WEEKLY, limit, subtotal, 0, List.of(), state);

        assertEquals(progress, view.progressPercent());
        assertEquals(balance, view.balance());
    }
}
