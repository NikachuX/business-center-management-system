import java.math.BigDecimal;

public interface RatePolicy {
    BigDecimal monthlyRate(Premise premise);
}
