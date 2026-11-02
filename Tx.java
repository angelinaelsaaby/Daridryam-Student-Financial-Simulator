import java.time.LocalDate;

final class Tx {
    private final int id;
    private final LocalDate date;
    private final String category;
    private final String description;
    private final double amount;
    private final boolean income;

    Tx(int id, LocalDate date, String category, String description, double amount, boolean income) {
        this.id = id;
        this.date = date;
        this.category = category;
        this.description = description;
        this.amount = amount;
        this.income = income;
    }

    int id() { return id; }
    LocalDate date() { return date; }
    String category() { return category; }
    String description() { return description; }
    double amount() { return amount; }
    boolean income() { return income; }
}
