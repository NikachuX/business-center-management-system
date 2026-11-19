import java.math.BigDecimal;

public final class Premise {
    private final long id;
    private final String number;
    private final int floor;
    private final BigDecimal area;
    private final BigDecimal baseMonthlyRate;
    private final String operationalStatus;

    public Premise(long id, String number, int floor, BigDecimal area,
                   BigDecimal baseMonthlyRate, String operationalStatus) {
        this.id = id;
        this.number = number;
        this.floor = floor;
        this.area = area;
        this.baseMonthlyRate = baseMonthlyRate;
        this.operationalStatus = operationalStatus;
    }

    public long id() { return id; }
    public String number() { return number; }
    public int floor() { return floor; }
    public BigDecimal area() { return area; }
    public BigDecimal baseMonthlyRate() { return baseMonthlyRate; }

    public boolean isAvailable() {
        return "available".equals(operationalStatus);
    }

    public BigDecimal monthlyCost(BigDecimal rate) {
        return rate.multiply(area);
    }
}
