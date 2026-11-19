import java.math.BigDecimal;

public final class BaseRatePolicy implements RatePolicy {
    @Override
    public BigDecimal monthlyRate(Premise premise) {
        return premise.baseMonthlyRate();
    }
}
