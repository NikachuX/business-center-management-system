import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RentalOffice {
    private final Map<Long, Premise> premises = new LinkedHashMap<>();
    private final List<Tenant> tenants = new ArrayList<>();
    private final Map<Long, Application> applications = new LinkedHashMap<>();
    private final List<Reservation> reservations = new ArrayList<>();
    private final Map<Long, CommercialOffer> offers = new LinkedHashMap<>();
    private final List<LeaseContract> contracts = new ArrayList<>();
    private long nextOfferItemId = 1;

    public RentalOffice(List<Premise> premises) {
        for (Premise premise : premises) {
            if (this.premises.putIfAbsent(premise.id(), premise) != null) {
                throw new IllegalArgumentException("повторяется идентификатор помещения");
            }
        }
    }

    public Application registerApplication(TenantData data, RentalRequest request, LocalDateTime now) {
        validateRegistration(data, request);

        String inn = data.inn().strip();
        Tenant tenant = tenants.stream()
                .filter(existing -> existing.data().inn().equals(inn))
                .findFirst()
                .orElse(null);
        if (tenant == null) {
            tenant = new Tenant(tenants.size() + 1L, data);
            tenants.add(tenant);
        }

        Application application = new Application(applications.size() + 1L, tenant, now, request);
        applications.put(application.id(), application);
        return application;
    }

    public List<Candidate> findAvailablePremises(Application application, LocalDateTime now) {
        requireRegistered(application);
        List<Occupancy> busy = new ArrayList<>();
        for (Reservation reservation : reservations) {
            if (reservation.blocksAt(now)) {
                busy.add(reservation);
            }
        }
        for (LeaseContract contract : contracts) {
            if (contract.blocksAt(now)) {
                busy.add(contract);
            }
        }

        RentalPeriod requested = application.requirements().period();
        List<Candidate> candidates = new ArrayList<>();
        for (Premise premise : premises.values()) {
            if (!application.accepts(premise)) {
                continue;
            }
            boolean occupied = busy.stream().anyMatch(period -> period.overlaps(premise, requested));
            if (!occupied) {
                candidates.add(new Candidate(premise, premise.monthlyCost(premise.baseMonthlyRate()),
                        application.matchesFloor(premise)));
            }
        }

        candidates.sort(Comparator.comparing(Candidate::floorMatch).reversed()
                .thenComparing(Candidate::monthlyCost));
        return List.copyOf(candidates);
    }

    public Reservation reservePremise(Application application, Premise premise, LocalDateTime now,
                                      Duration holdPeriod) {
        requireRegistered(application);
        requireKnownPremise(premise);
        if (!premise.isAvailable()) {
            throw new IllegalArgumentException("помещение недоступно для сдачи");
        }
        RentalPeriod requested = application.requirements().period();
        requested.checkOrder();

        for (Reservation reservation : reservations) {
            if (reservation.blocksAt(now) && reservation.overlaps(premise, requested)) {
                throw new IllegalArgumentException("конфликт с действующим резервом (id="
                        + reservation.id() + ")");
            }
        }
        for (LeaseContract contract : contracts) {
            if (contract.blocksAt(now) && contract.overlaps(premise, requested)) {
                throw new IllegalArgumentException("конфликт с заключённым договором аренды");
            }
        }

        Reservation reservation = new Reservation(reservations.size() + 1L, application,
                premise, now, holdPeriod);
        reservations.add(reservation);
        return reservation;
    }

    public CommercialOffer generateCommercialOffer(Application application, List<Premise> selected,
                                                   LocalDateTime now, int validDays, RatePolicy policy) {
        requireRegistered(application);
        if (selected.isEmpty()) {
            throw new IllegalArgumentException("не выбрано ни одного помещения");
        }
        Map<Long, BigDecimal> rates = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        for (Premise premise : selected) {
            requireKnownPremise(premise);
            BigDecimal rate = policy.monthlyRate(premise);
            if (rate == null || rate.signum() <= 0) {
                errors.add("ставка для помещения " + premise.number() + " должна быть положительным числом");
            }
            rates.put(premise.id(), rate);
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }
        application.requirements().period().checkOrder();

        CommercialOffer offer = new CommercialOffer(offers.size() + 1L, application, now,
                validDays, nextOfferItemId, selected, rates);
        offers.put(offer.id(), offer);
        nextOfferItemId += offer.items().size();
        return offer;
    }

    public LeaseContract prepareContract(OfferItem item, String number, LocalDateTime now) {
        CommercialOffer offer = item.offer();
        if (offers.get(offer.id()) != offer || !offer.items().contains(item)) {
            throw new IllegalArgumentException("не найдена позиция предложения договора");
        }
        if (contracts.stream().anyMatch(contract -> contract.item() == item)) {
            throw new IllegalArgumentException("Для выбранной позиции уже существует договор");
        }

        LeaseContract contract = new LeaseContract(contracts.size() + 1L, item, number, now);
        contracts.add(contract);
        return contract;
    }

    public LeaseContract updatePremiseAndLease(LeaseContract contract, LocalDateTime now) {
        if (!contracts.contains(contract)) {
            throw new IllegalArgumentException("нарушена цепочка связей договора");
        }
        for (LeaseContract existing : contracts) {
            if (existing != contract && existing.blocksAt(now)
                    && existing.overlaps(contract.premise(), contract.period())) {
                throw new IllegalArgumentException("конфликт с заключённым договором аренды");
            }
        }
        contract.sign(now);
        contract.item().offer().application().markContractSigned();
        for (Reservation reservation : reservations) {
            reservation.finishAfterSigning(contract);
        }
        return contract;
    }

    public List<Tenant> tenants() { return List.copyOf(tenants); }
    public List<Application> applications() { return List.copyOf(applications.values()); }
    public List<Reservation> reservations() { return List.copyOf(reservations); }
    public List<CommercialOffer> offers() { return List.copyOf(offers.values()); }
    public List<LeaseContract> contracts() { return List.copyOf(contracts); }

    private void requireRegistered(Application application) {
        if (applications.get(application.id()) != application) {
            throw new IllegalArgumentException("заявка не зарегистрирована");
        }
    }

    private void requireKnownPremise(Premise premise) {
        if (premises.get(premise.id()) != premise) {
            throw new IllegalArgumentException("помещение не найдено");
        }
    }

    private static void validateRegistration(TenantData data, RentalRequest request) {
        List<String> errors = new ArrayList<>();
        requireField(errors, data.name(), "арендатора", "name");
        requireField(errors, data.inn(), "арендатора", "inn");
        requireField(errors, data.contactPerson(), "арендатора", "contact_person");
        requireField(errors, data.phone(), "арендатора", "phone");

        if (data.inn() != null && !data.inn().isBlank()
                && !data.inn().strip().matches("(?U)(?:\\d{10}|\\d{12})")) {
            errors.add("ИНН должен состоять из 10 или 12 цифр");
        }
        if (data.email() != null && !data.email().isBlank() && !data.email().matches("[^@]+@[^@]*\\.[^@]*")) {
            errors.add("некорректный email");
        }

        RentalPeriod period = request.period();
        requireField(errors, request.activityType(), "заявки", "activity_type");
        requireField(errors, request.minArea(), "заявки", "min_area");
        requireField(errors, request.maxArea(), "заявки", "max_area");
        requireField(errors, request.maxMonthlyBudget(), "заявки", "max_monthly_budget");
        requireField(errors, period == null ? null : period.start(), "заявки", "rental_start_date");
        requireField(errors, period == null ? null : period.end(), "заявки", "rental_end_date");
        requirePositive(errors, request.minArea(), "min_area");
        requirePositive(errors, request.maxArea(), "max_area");
        requirePositive(errors, request.maxMonthlyBudget(), "max_monthly_budget");
        if (errors.isEmpty()) {
            if (request.minArea().compareTo(request.maxArea()) > 0) {
                errors.add("минимальная площадь больше максимальной");
            }
            if (period.start().isAfter(period.end())) {
                errors.add("дата начала аренды позже даты окончания");
            }
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }
    }

    private static void requireField(List<String> errors, Object value, String label, String field) {
        if (value == null || (value instanceof String text && text.isBlank())) {
            errors.add("не заполнено поле " + label + ": " + field);
        }
    }

    private static void requirePositive(List<String> errors, BigDecimal value, String field) {
        if (value != null && value.signum() <= 0) {
            errors.add("поле заявки " + field + " должно быть положительным");
        }
    }
}
