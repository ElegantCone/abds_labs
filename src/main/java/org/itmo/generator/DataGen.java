package org.itmo.generator;

import lombok.Builder;
import lombok.NonNull;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class DataGen {
    static final int SEED = 42;
    static Random random = new Random(SEED);
    static int realtyCapacity = 1000;
    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final LocalDate FIRST_READING_DATE = LocalDate.of(2023, 11, 1);
    private static final LocalDate LAST_READING_DATE = LocalDate.of(2026, 10, 31);
    private static final YearMonth FIRST_INVOICE_MONTH = YearMonth.of(2023, 11);
    private static final YearMonth LAST_INVOICE_MONTH = YearMonth.of(2026, 10);
    private static final LocalDateTime OPEN_VALID_TO = LocalDateTime.of(2099, 1, 1, 0, 0);


    public static void main(String[] args) throws Exception {
        Path dataDirectory = Path.of(args.length == 0 ? "data" : args[0]);
        Files.createDirectories(dataDirectory);

        random = new Random(SEED);
        realties.clear();
        replacements.clear();
        profiles.clear();
        monthlyReadings.clear();
        initRealties();
        buildGaugeReplacements();

        generateRealties(dataDirectory);
        generateUsers(dataDirectory);
        generateGauges(dataDirectory);
        generateTariffs(dataDirectory);
        generateGaugeBindings(dataDirectory);
        generateMeterReadings(dataDirectory);
        generateInvoices(dataDirectory);
    }

    @Builder
    record Realty(long id, String address, int square, String type, String number, int rooms, int floor, int porch,
                  long ownerId, long tariffId, LocalDateTime createdAt) {
        public static String getColumns() {
            return "id,address,square,type,number,rooms,floor,porch,created_at";
        }

        public boolean isCommercial() {
            return Objects.equals(type, "nrcp");
        }

        @Override
        @NonNull
        public String toString() {
            return DataGen.escapeCsvSymbolsAndBuildLine(
                    id,
                    address,
                    square,
                    type,
                    number,
                    rooms,
                    floor,
                    porch,
                    formatter.format(createdAt)
            );
        }
    }

    static List<Realty> realties = new ArrayList<>(realtyCapacity);

    public static void initRealties() {
        List<Integer> owners = new ArrayList<>();
        for (int id = 1; id <= realtyCapacity; id++) owners.add(id);
        Collections.shuffle(owners, random);
        int[] numbers = new int[addresses.size()];
        for (int idx = 1; idx <= realtyCapacity; idx++) {
            var realty = Realty.builder().id(idx).ownerId(owners.get(idx - 1));
            int house = random.nextInt(addresses.size());
            var address = addresses.get(house);
            realty.address(address.address);
            var number = ++numbers[house];
            realty.number(String.valueOf(number));

            var type = random.nextDouble() < 0.12 ? "nrcp" : "flat";
            realty.type(type);
            realty.tariffId(type.equals("nrcp") ? 3 : (random.nextDouble() < 0.75 ? 1 : 2));
            realty.createdAt(randomDateTime(LocalDate.of(2018, 1, 1), LocalDate.of(2022, 12, 31)));
            switch (type) {
                case "nrcp": {
                    realty.number("")
                            .square(Math.max(20, choice(address.commercial()) + random.nextInt(-15, 31)))
                            .rooms(0)
                            .floor(1)
                            .porch(0);
                    break;
                }
                case "flat": {
                    realty.square(Math.max(18, choice(address.flats()) + random.nextInt(-7, 12)))
                            .rooms(realty.square > 90 ? 4 : (realty.square > 65 ? 3 : (realty.square > 40 ? 2 : 1)))
                            .floor(random.nextInt(2, address.floors + 1))
                            .porch(random.nextInt(1, address.porches + 1));
                }
            }
            realties.add(realty.build());
            profiles.put((long) idx, new ConsumptionProfile(
                    random.nextDouble(0.6, 1.6), random.nextDouble(0.45, 0.8),
                    random.nextDouble(0.1, 0.35)));
        }
    }

    private static void generateRealties(Path dataDirectory) throws IOException {
        try (var writer = createWriter(dataDirectory, "dim_realty.csv", Realty.getColumns())) {
            for (Realty realty : realties) {
                write(writer, realty.toString());
            }
        }
    }

    record GaugeReplacement(long newGaugeId, LocalDate replacementDate) {
    }

    static Map<Long, GaugeReplacement> replacements = new LinkedHashMap<>();

    record ConsumptionProfile(double scale, double dayShare, double seasonality) { }
    record MonthlyKey(long realtyId, YearMonth month) { }
    static Map<Long, ConsumptionProfile> profiles = new HashMap<>();
    static Map<MonthlyKey, long[]> monthlyReadings = new HashMap<>();

    private static void buildGaugeReplacements() {
        int replacementNumber = 0;

        var candidates = new ArrayList<>(realties);
        Collections.shuffle(candidates, random);
        for (Realty realty : candidates.subList(0, realtyCapacity / 10)) {
            replacementNumber++;
            // Замена с первого числа: один счётчик на каждый расчётный месяц.
            LocalDate date = FIRST_INVOICE_MONTH.plusMonths(random.nextInt(1, 36)).atDay(1);
            replacements.put(realty.id(), new GaugeReplacement(realtyCapacity + replacementNumber, date));
        }
    }

    record Gauge(String name, String country) {
    }

    static List<Gauge> gaugeModels = List.of(
            new Gauge("Инкотекс Меркурий 230", "Россия"),
            new Gauge("Тайпит НЕВА МТ 324", "Россия"),
            new Gauge("Tuya ZMAi-90", "China"),
            new Gauge("Энергомера CE102", "Россия"),
            new Gauge("Матрица NP73E", "Россия"),
            new Gauge("Landis+Gyr E450", "Швейцария")
    );

    static void generateGauges(Path dataDirectory) throws IOException {
        try (var writer = createWriter(dataDirectory, "dim_gauges.csv", "id,name,country,created_at")) {
            for (Realty realty : realties) {
                Gauge gauge = choice(gaugeModels);
                write(writer, escapeCsvSymbolsAndBuildLine(realty.id(), gauge.name(), gauge.country(), formatter.format(installedAt(realty))));
            }

            for (GaugeReplacement replacement : replacements.values()) {
                Gauge gauge = choice(gaugeModels);
                LocalDateTime createdAt = replacement
                        .replacementDate()
                        .atStartOfDay();
                write(writer, escapeCsvSymbolsAndBuildLine(replacement.newGaugeId(), gauge.name(), gauge.country(), formatter.format(createdAt)));
            }
        }
    }

    record AddressData(String address, Integer porches, Integer floors, List<Integer> flats, List<Integer> commercial) {
    }

    static List<AddressData> addresses = List.of(
            new AddressData("г. Москва, улица Пушкина, дом Колотушкина", 4, 20, List.of(40, 40, 65, 90), List.of(100, 50, 40)),
            new AddressData("г. Москва, проспект Мира, дом Кефира", 2, 14, List.of(33, 33, 55, 72), List.of(60, 70)),
            new AddressData("г. Москва, улица Есенина, дом Каруселина", 7, 5, List.of(45, 45, 68, 83, 102), List.of(500)),
            new AddressData("Г. Москва, улица Мышкина, дом Камышкина", 5, 9, List.of(18, 18, 41, 54), List.of(50, 60))
    );


    private record TariffVersion(long id, String name, int priceDay, int priceNight, LocalDateTime validFrom,
                                 LocalDateTime validTo) {
        public static String getColumns() {
            return "id,name,price_day_kwph,price_night_kwph,valid_from,valid_to,created_at,updated_at";
        }

        @Override
        @NonNull
        public String toString() {
            return DataGen.escapeCsvSymbolsAndBuildLine(
                    id,
                    name,
                    priceDay,
                    priceNight,
                    formatter.format(validFrom),
                    formatter.format(validTo),
                    formatter.format(validFrom),
                    formatter.format(validFrom));
        }
    }

    static List<TariffVersion> tariffs = buildTariffs();

    private static List<TariffVersion> buildTariffs() {
        List<TariffVersion> result = new ArrayList<>();
        String[] names = {"День–Ночь", "Базовый", "Коммерческий"};
        int[] dayPrices = {600, 560, 820};
        int[] nightPrices = {300, 560, 510};
        for (int id = 1; id <= names.length; id++) {
            for (int version = 0; version < 7; version++) {
                var start = LocalDateTime.of(2023, 7, 1, 0, 0).plusMonths(version * 6L);
                var end = version == 6 ? OPEN_VALID_TO : start.plusMonths(6);
                result.add(new TariffVersion(id, names[id - 1],
                        dayPrices[id - 1] + version * 25,
                        nightPrices[id - 1] + version * 25, start, end));
            }
        }
        return result;
    }

    private static void generateTariffs(Path dataDirectory) throws IOException {
        try (var writer = createWriter(dataDirectory, "dim_tariffs.csv", TariffVersion.getColumns())) {
            for (var tariff : tariffs) {
                write(writer, tariff.toString());
            }
        }
    }

    @Builder
    record User(long id, String fio, String phone, String prevPhone, LocalDateTime createdAt, LocalDateTime updatedAt) {
        public static String getColumns() {
            return "id,fio,phone,phone_prev,created_at,updated_at";
        }

        public static String makeName(String last, String first, String patronymic) {
            return last + " " + first + " " + patronymic;
        }

        @Override
        @NonNull
        public String toString() {
            return DataGen.escapeCsvSymbolsAndBuildLine(id,
                    fio,
                    phone,
                    prevPhone,
                    formatter.format(createdAt),
                    formatter.format(updatedAt));
        }
    }

    private static void generateUsers(Path dataDirectory) throws IOException {
        String[] lastNames = {
                "Иванов", "Петров", "Сидоров", "Смирнов",
                "Кузнецов", "Попов", "Васильев", "Соколов",
                "Морозов", "Волков", "Лебедев", "Козлов", "Новиков", "Орлов",
                "Макаров", "Захаров", "Зайцев", "Павлов", "Семёнов", "Фёдоров"
        };

        String[] firstNames = {
                "Александр", "Михаил", "Иван", "Дмитрий", "Алексей",
                "Сергей", "Андрей", "Николай", "Павел", "Игорь", "Владимир", "Евгений"
        };

        String[] patronymics = {
                "Иванович", "Петрович", "Сергеевич", "Александрович",
                "Игоревич", "Михайлович", "Андреевич", "Николаевич",
                "Павлович", "Владимирович", "Евгеньевич", "Дмитриевич"
        };
        String[] femaleNames = {"Анна", "Елена", "Мария", "Ольга", "Наталья", "Ирина",
                "Татьяна", "Светлана", "Юлия", "Екатерина", "Дарья", "Анастасия"};
        Set<String> phones = new HashSet<>();

        try (var writer = createWriter(dataDirectory, "dim_users.csv", User.getColumns())) {
            for (Realty realty : realties) {
                long id = realty.ownerId();
                boolean female = random.nextBoolean();
                String last = lastNames[random.nextInt(lastNames.length)] + (female ? "а" : "");
                String patronymic = patronymics[random.nextInt(patronymics.length)];
                if (female) patronymic = patronymic.substring(0, patronymic.length() - 2) + "на";
                var createdAt = realty.createdAt().plusDays(random.nextInt(1, 90));
                boolean changedPhone = random.nextDouble() < 0.2;
                var user = User.builder().id(id)
                        .fio(User.makeName(last,
                                female ? femaleNames[random.nextInt(femaleNames.length)]
                                        : firstNames[random.nextInt(firstNames.length)], patronymic))
                        .createdAt(createdAt)
                        .updatedAt(changedPhone ? randomDateTime(LocalDate.of(2024, 1, 1), LAST_READING_DATE) : createdAt)
                        .phone(randomPhone(phones))
                        .prevPhone(changedPhone ? randomPhone(phones) : "");

                write(writer, user.build().toString());
            }
        }
    }

    private static void generateGaugeBindings(Path dataDirectory) throws IOException {
        try (var writer = createWriter(dataDirectory, "fact_gauge_bindings.csv", "gauge_id,realty_id,created_at,sign,version")) {
            for (Realty realty : realties) {
                write(writer, escapeCsvSymbolsAndBuildLine(realty.id(), realty.id(), formatter.format(installedAt(realty)), 1, 1));

                GaugeReplacement replacement = replacements.get(realty.id());
                if (replacement == null) continue;

                LocalDateTime replacementTime = replacement
                        .replacementDate()
                        .atStartOfDay();
                // Аннулирующая строка повторяет исходное состояние и его версию.
                write(writer, escapeCsvSymbolsAndBuildLine(realty.id(), realty.id(), formatter.format(installedAt(realty)), -1, 1));
                write(writer, escapeCsvSymbolsAndBuildLine(replacement.newGaugeId(), realty.id(), formatter.format(replacementTime), 1, 2));
            }
        }
    }

    @Builder
    record Meter(long id, long userId, long tariffId, long gaugeId, LocalDate readingDate, long dayKw, long nightKw,
                 LocalDateTime createdAt) {
        public static String getColumns() {
            return "realty_id,user_id,tariff_id,gauge_id,reading_date,day_kw,night_kw,created_at";
        }

        @Override
        @NonNull
        public String toString() {
            return escapeCsvSymbolsAndBuildLine(
                    id,
                    userId,
                    tariffId,
                    gaugeId,
                    readingDate,
                    dayKw,
                    nightKw,
                    formatter.format(createdAt));
        }
    }

    private static void generateMeterReadings(Path dataDirectory) throws IOException {
        try (var writer = createWriter(dataDirectory, "fact_meter_readings.csv", Meter.getColumns())) {
            for (Realty realty : realties) {
                long dayCumulative = random.nextInt(500, 30_001);
                long nightCumulative = random.nextInt(200, 15_001);
                double dayShare = profiles.get(realty.id()).dayShare();

                GaugeReplacement replacement = replacements.get(realty.id());

                for (var date = FIRST_READING_DATE; !date.isAfter(LAST_READING_DATE); date = date.plusDays(1)) {
                    var meter = Meter.builder()
                            .id(realty.id())
                            .userId(realty.ownerId)
                            .tariffId(realty.tariffId)
                            .readingDate(date);

                    if (replacement != null && date.equals(replacement.replacementDate())) {
                        dayCumulative = 0;
                        nightCumulative = 0;
                    }

                    meter.gaugeId(gaugeIdFor(realty, replacement, date));

                    int totalConsumption = dailyConsumption(realty, date);

                    int dayConsumption = Math.max(0, (int) Math.round(totalConsumption
                            * Math.clamp(dayShare + random.nextDouble(-0.08, 0.08), 0.25, 0.9)));
                    var totals = monthlyReadings.computeIfAbsent(
                            new MonthlyKey(realty.id(), YearMonth.from(date)), key -> new long[2]);
                    totals[0] += dayConsumption;
                    totals[1] += totalConsumption - dayConsumption;

                    dayCumulative += dayConsumption;
                    nightCumulative += Math.max(0, totalConsumption - dayConsumption);

                    LocalDateTime originalCreatedAt = date.atTime(random.nextInt(6, 20), random.nextInt(60), random.nextInt(60));

                    if (random.nextDouble() < 0.03) {
                        meter
                                .dayKw(Math.max(0, dayCumulative + random.nextInt(-20, 51)))
                                .nightKw(Math.max(0, nightCumulative + random.nextInt(-10, 31)))
                                .createdAt(originalCreatedAt);

                        write(writer, meter.build().toString());

                        meter
                                .createdAt(originalCreatedAt.plusMinutes(random.nextInt(10, 181)))
                                .dayKw(dayCumulative)
                                .nightKw(nightCumulative);


                        write(writer, meter.build().toString());
                    } else {
                        meter
                                .createdAt(originalCreatedAt)
                                .dayKw(dayCumulative)
                                .nightKw(nightCumulative);
                        write(writer, meter.build().toString());
                    }
                }
            }
        }
    }

    private static long gaugeIdFor(Realty realty, GaugeReplacement replacement, LocalDate date) {
        if (replacement != null && !date.isBefore(replacement.replacementDate())) {
            return replacement.newGaugeId();
        }
        return realty.id();
    }

    private static int dailyConsumption(Realty realty, LocalDate date) {
        var profile = profiles.get(realty.id());
        double monthlyBase = realty.square() * (realty.isCommercial() ? 6.0 : 3.5) * profile.scale();
        double dailyBase = monthlyBase / date.lengthOfMonth();
        double seasonal = 1 + profile.seasonality() * Math.cos((date.getDayOfYear() - 15) * 2 * Math.PI / date.lengthOfYear());
        boolean weekend = date.getDayOfWeek().getValue() >= 6;
        double weekly = weekend ? (realty.isCommercial() ? 0.55 : 1.15) : 1;
        double vacant = random.nextDouble() < 0.04 ? 0.15 : 1;
        return Math.max(0, (int) Math.round(dailyBase * seasonal * weekly * vacant * random.nextDouble(0.55, 1.5)));
    }

    @Builder
    record Invoice(long id, long userId, long tariffId, long gaugeId, long dayKw, long nightKw, long price,
                   LocalDate deadline, LocalDateTime createdAt) {
        public static String getColumns() {
            return "realty_id,user_id,tariff_id,gauge_id,day_kw,night_kw,price,deadline,created_at";
        }

        @Override
        @NonNull
        public String toString() {
            return escapeCsvSymbolsAndBuildLine(
                    id,
                    userId,
                    tariffId,
                    gaugeId,
                    dayKw,
                    nightKw,
                    price,
                    deadline,
                    formatter.format(createdAt)
            );
        }
    }

    private static void generateInvoices(Path dataDirectory) throws IOException {
        try (var writer = createWriter(dataDirectory, "fact_invoice.csv", Invoice.getColumns())) {
            for (Realty realty : realties) {
                GaugeReplacement replacement = replacements.get(realty.id());

                for (var month = FIRST_INVOICE_MONTH; !month.isAfter(LAST_INVOICE_MONTH); month = month.plusMonths(1)) {
                    var invoice = Invoice.builder()
                            .id(realty.id()).userId(realty.ownerId())
                            .createdAt(month.atEndOfMonth().atTime(23, random.nextInt(30, 60), random.nextInt(60)))
                            .deadline(month.plusMonths(1).atDay(10));

                    long[] consumption = monthlyReadings.get(new MonthlyKey(realty.id(), month));
                    TariffVersion tariff = tariffFor(realty.tariffId(), invoice.createdAt);

                    invoice
                            .gaugeId(gaugeIdFor(realty, replacement, invoice.createdAt.toLocalDate()))
                            .dayKw(consumption[0])
                            .nightKw(consumption[1])
                            .tariffId(tariff.id())
                            .price(invoice.dayKw * tariff.priceDay + invoice.nightKw * tariff.priceNight());

                    write(writer, invoice.build().toString());
                }
            }
        }
    }

    private static TariffVersion tariffFor(long tariffId, LocalDateTime date) {
        for (TariffVersion tariff : tariffs) {
            if (tariff.id() == tariffId && !date.isBefore(tariff.validFrom()) && date.isBefore(tariff.validTo())) {
                return tariff;
            }
        }
        throw new IllegalArgumentException("Invalid tariff date: " + date);
    }

    private static <T> T choice(List<T> values) {
        return values.get(random.nextInt(values.size()));
    }

    private static LocalDateTime randomDateTime(LocalDate first, LocalDate last) {
        return LocalDate.ofEpochDay(random.nextLong(first.toEpochDay(), last.toEpochDay() + 1))
                .atTime(random.nextInt(8, 21), random.nextInt(60), random.nextInt(60));
    }

    private static LocalDateTime installedAt(Realty realty) {
        return realty.createdAt().plusDays(100);
    }

    private static String randomPhone(Set<String> used) {
        String phone;
        do {
            phone = "+79" + String.format(Locale.ROOT, "%09d", random.nextLong(1_000_000_000L));
        } while (!used.add(phone));
        return phone;
    }

    private static BufferedWriter createWriter(Path dataDirectory, String fileName, String columns) throws IOException {
        var writer = Files.newBufferedWriter(dataDirectory.resolve(fileName));
        writer.write(columns);
        writer.newLine();
        return writer;
    }

    private static void write(BufferedWriter writer, String str) throws IOException {
        writer.write(str);
        writer.newLine();
    }

    public static String escapeCsvSymbolsAndBuildLine(Object... parts) {
        StringBuilder sb = new StringBuilder();
        for (var i = 0; i < parts.length; i++) {
            if (i != 0) sb.append(",");

            var str = parts[i].toString();
            if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
                sb.append("\"").append(str.replace("\"", "\"\"")).append("\"");
            } else {
                sb.append(str);
            }
        }
        return sb.toString();
    }
}
