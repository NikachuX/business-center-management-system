import java.time.LocalDateTime;

public abstract class Occupancy {
    public abstract Premise premise();
    public abstract RentalPeriod period();

    protected abstract boolean blocksAt(LocalDateTime now);

    public final boolean overlaps(Premise otherPremise, RentalPeriod otherPeriod) {
        return premise().id() == otherPremise.id() && period().overlaps(otherPeriod);
    }
}
