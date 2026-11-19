import java.time.LocalDateTime;

public final class LeaseContract extends Occupancy {
    private final long id;
    private final OfferItem item;
    private final String number;
    private final LocalDateTime createdAt;
    private LocalDateTime signedAt;
    private String status = "draft";

    public LeaseContract(long id, OfferItem item, String number, LocalDateTime createdAt) {
        this.id = id;
        this.item = item;
        this.number = number;
        this.createdAt = createdAt;
    }

    public long id() { return id; }
    public OfferItem item() { return item; }
    public String number() { return number; }
    public LocalDateTime createdAt() { return createdAt; }
    public LocalDateTime signedAt() { return signedAt; }
    public String status() { return status; }

    @Override
    public Premise premise() { return item.premise(); }

    @Override
    public RentalPeriod period() { return item.period(); }

    @Override
    protected boolean blocksAt(LocalDateTime now) {
        return "signed".equals(status);
    }

    public void sign(LocalDateTime now) {
        if (blocksAt(now)) {
            throw new IllegalArgumentException("договор уже заключён");
        }
        signedAt = now;
        status = "signed";
    }
}
