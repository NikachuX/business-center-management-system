import java.math.BigDecimal;

public record Candidate(Premise premise, BigDecimal monthlyCost, boolean floorMatch) {}
