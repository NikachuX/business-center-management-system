import java.math.BigDecimal;

public final class OfferItem {
    private final long id;
    private final CommercialOffer offer;
    private final Premise premise;
    private final BigDecimal monthlyRate;
    private final RentalPeriod period;

    OfferItem(long id, CommercialOffer offer, Premise premise, BigDecimal monthlyRate) {
        this.id = id;
        this.offer = offer;
        this.premise = premise;
        this.monthlyRate = monthlyRate;
        this.period = offer.application().requirements().period();
    }

    public long id() { return id; }
    public CommercialOffer offer() { return offer; }
    public Premise premise() { return premise; }
    public BigDecimal monthlyRate() { return monthlyRate; }
    public RentalPeriod period() { return period; }

    public BigDecimal monthlyCost() {
        return premise.monthlyCost(monthlyRate);
    }

    public long months() {
        return period.months();
    }

    public BigDecimal totalCost() {
        return monthlyCost().multiply(BigDecimal.valueOf(months()));
    }
}
