import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CommercialOffer {
    private final long id;
    private final Application application;
    private final LocalDateTime createdAt;
    private final LocalDateTime validUntil;
    private final String status = "formed";
    private final List<OfferItem> items;

    CommercialOffer(long id, Application application, LocalDateTime createdAt, int validDays,
                    long firstItemId, List<Premise> selected, Map<Long, BigDecimal> rates) {
        this.id = id;
        this.application = application;
        this.createdAt = createdAt;
        this.validUntil = createdAt.plusDays(validDays);

        Set<Long> seenPremises = new HashSet<>();
        List<OfferItem> positions = new ArrayList<>();
        long itemId = firstItemId;
        for (Premise premise : selected) {
            if (!seenPremises.add(premise.id())) {
                throw new IllegalArgumentException(
                        "помещение " + premise.number() + " указано в предложении повторно");
            }
            positions.add(new OfferItem(itemId++, this, premise, rates.get(premise.id())));
        }
        items = List.copyOf(positions);
    }

    public long id() { return id; }
    public Application application() { return application; }
    public LocalDateTime createdAt() { return createdAt; }
    public LocalDateTime validUntil() { return validUntil; }
    public String status() { return status; }
    public List<OfferItem> items() { return items; }

    public BigDecimal totalCost() {
        BigDecimal total = BigDecimal.ZERO;
        for (OfferItem item : items) {
            total = total.add(item.totalCost());
        }
        return total;
    }
}
