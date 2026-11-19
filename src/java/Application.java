import java.time.LocalDateTime;

public final class Application {
    private final long id;
    private final Tenant tenant;
    private final LocalDateTime createdAt;
    private final RentalRequest requirements;
    private String status = "new";

    public Application(long id, Tenant tenant, LocalDateTime createdAt,
                       RentalRequest requirements) {
        this.id = id;
        this.tenant = tenant;
        this.createdAt = createdAt;
        this.requirements = requirements;
    }

    public long id() { return id; }
    public Tenant tenant() { return tenant; }
    public LocalDateTime createdAt() { return createdAt; }
    public RentalRequest requirements() { return requirements; }
    public String status() { return status; }

    public boolean accepts(Premise premise) {
        return premise.isAvailable()
                && premise.area().compareTo(requirements.minArea()) >= 0
                && premise.area().compareTo(requirements.maxArea()) <= 0
                && premise.monthlyCost(premise.baseMonthlyRate())
                          .compareTo(requirements.maxMonthlyBudget()) <= 0;
    }

    public boolean matchesFloor(Premise premise) {
        Integer floor = requirements.preferredFloor();
        return floor == null || floor == premise.floor();
    }

    public void markContractSigned() {
        status = "contract_signed";
    }
}
