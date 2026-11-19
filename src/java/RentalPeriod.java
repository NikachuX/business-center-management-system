import java.time.LocalDate;

public record RentalPeriod(LocalDate start, LocalDate end) {
    public void checkOrder() {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("дата начала аренды позже даты окончания");
        }
    }

    public boolean overlaps(RentalPeriod other) {
        return !start.isAfter(other.end()) && !other.start().isAfter(end);
    }

    public long months() {
        LocalDate endExclusive = end.plusDays(1);
        long months = (endExclusive.getYear() - (long) start.getYear()) * 12
                + endExclusive.getMonthValue() - start.getMonthValue();
        if (endExclusive.getDayOfMonth() > start.getDayOfMonth()) {
            months++;
        }
        return Math.max(1, months);
    }
}
