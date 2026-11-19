import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ProposedRatePolicy implements RatePolicy {
    private final Map<Long, BigDecimal> rates;

    public ProposedRatePolicy(Map<Long, BigDecimal> rates) {
        this.rates = Collections.unmodifiableMap(new HashMap<>(rates));
    }

    @Override
    public BigDecimal monthlyRate(Premise premise) {
        return rates.getOrDefault(premise.id(), premise.baseMonthlyRate());
    }
}
