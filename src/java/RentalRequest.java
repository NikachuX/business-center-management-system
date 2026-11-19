import java.math.BigDecimal;

public record RentalRequest(String activityType, BigDecimal minArea, BigDecimal maxArea,
                            Integer preferredFloor, BigDecimal maxMonthlyBudget,
                            RentalPeriod period) {}
