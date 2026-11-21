import java.time.Duration;
import java.time.LocalDateTime;

public final class Reservation extends Occupancy {
    private final long id;
    private final Application application;
    private final Premise premise;
    private final LocalDateTime createdAt;
    private final RentalPeriod period;
    private final LocalDateTime expiresAt;
    private String status = "active";

    public Reservation(long id, Application application, Premise premise,
                       LocalDateTime createdAt, Duration holdPeriod) {
        this.id = id;
        this.application = application;
        this.premise = premise;
        this.createdAt = createdAt;
        this.period = application.requirements().period();
        this.expiresAt = createdAt.plus(holdPeriod);
    }

    public long id() { return id; }
    public Application application() { return application; }
    public LocalDateTime createdAt() { return createdAt; }
    public LocalDateTime expiresAt() { return expiresAt; }
    public String status() { return status; }

    @Override
    public Premise premise() { return premise; }

    @Override
    public RentalPeriod period() { return period; }

    @Override
    protected boolean blocksAt(LocalDateTime now) {
        return "active".equals(status) && expiresAt.isAfter(now);
    }

    public void finishAfterSigning(LeaseContract contract) {
        LocalDateTime signedAt = contract.signedAt();
        if (signedAt != null && application == contract.item().offer().application()
                && blocksAt(signedAt)) {
            status = premise.id() == contract.premise().id() ? "converted" : "cancelled";
        }
    }
}
