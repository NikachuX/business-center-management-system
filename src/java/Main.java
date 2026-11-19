import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class Main {
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");

    private Main() {}

    public static void main(String[] args) {
        try {
            runDemo();
        } catch (IllegalArgumentException error) {
            System.err.println("Ошибка: " + error.getMessage());
            System.exit(1);
        }
    }

    private static void runDemo() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 2, 10, 0);
        RentalOffice office = new RentalOffice(List.of(
                new Premise(1, "201", 2, decimal(50), decimal(1200), "available"),
                new Premise(2, "301", 3, decimal(60), decimal(900), "available"),
                new Premise(3, "202", 2, decimal(45), decimal(1000), "maintenance")));

        TenantData tenant = new TenantData("ООО Пример", "7700000001",
                "Иван Иванов", "+79000000001", "office@example.ru");
        RentalRequest request = new RentalRequest("Разработка программного обеспечения",
                decimal(40), decimal(70), 2, decimal(80000),
                new RentalPeriod(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31)));

        System.out.println("1. Регистрация заявки");
        Application application = office.registerApplication(tenant, request, now);
        System.out.printf("Арендатор: %s; заявка: %d; статус: %s%n",
                application.tenant().data().name(), application.id(), application.status());

        System.out.println("\n2. Подбор свободных помещений");
        List<Candidate> candidates = office.findAvailablePremises(application, now);
        for (Candidate candidate : candidates) {
            Premise premise = candidate.premise();
            System.out.printf(Locale.ROOT,
                    "Помещение %s; этаж %d; стоимость %.2f руб./мес.; совпадение этажа: %s%n",
                    premise.number(), premise.floor(), candidate.monthlyCost(),
                    candidate.floorMatch() ? "да" : "нет");
        }
        if (candidates.isEmpty()) {
            System.out.println("Подходящих помещений нет. Демонстрация завершена.");
            return;
        }
        Premise selected = candidates.getFirst().premise();

        System.out.println("\n3. Резервирование выбранного помещения");
        Reservation reservation = office.reservePremise(application, selected, now, Duration.ofDays(3));
        System.out.printf("Резерв: %d; помещение: %s; действует до: %s%n",
                reservation.id(), selected.number(), reservation.expiresAt().format(TIMESTAMP));

        System.out.println("\n4. Формирование коммерческого предложения");
        CommercialOffer offer = office.generateCommercialOffer(application, List.of(selected),
                now, 14, new BaseRatePolicy());
        OfferItem item = offer.items().getFirst();
        System.out.printf("Предложение: %d; позиция: %d; месяцев: %d%n",
                offer.id(), item.id(), item.months());
        System.out.printf(Locale.ROOT, "Стоимость в месяц: %.2f руб.; за период: %.2f руб.%n",
                item.monthlyCost(), item.totalCost());

        LeaseContract contract = office.prepareContract(item, "АР-001", now);
        LocalDateTime signedAt = now.plusDays(1);
        System.out.println("\n5. Фиксация подписания договора");
        office.updatePremiseAndLease(contract, signedAt);
        System.out.printf("Договор: %s; статус: %s%n", contract.number(), contract.status());
        System.out.printf("Статус заявки: %s; статус резерва: %s%n",
                application.status(), reservation.status());
        System.out.printf("Арендатор: %s; помещение: %s%n",
                application.tenant().data().name(), contract.premise().number());

        System.out.println("\nПроверка доступности после заключения договора");
        List<String> remaining = office.findAvailablePremises(application, signedAt).stream()
                .map(candidate -> candidate.premise().number()).toList();
        System.out.println("Доступные варианты: " + (remaining.isEmpty() ? "нет" : String.join(", ", remaining)));
        int itemCount = office.offers().stream().mapToInt(entry -> entry.items().size()).sum();
        System.out.printf("Сохранено: арендаторов %d; заявок %d; резервов %d; предложений %d; "
                        + "позиций %d; договоров %d%n",
                office.tenants().size(), office.applications().size(), office.reservations().size(),
                office.offers().size(), itemCount, office.contracts().size());
    }

    private static BigDecimal decimal(long number) {
        return BigDecimal.valueOf(number);
    }
}
