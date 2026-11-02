import java.time.LocalDate;

final class Profile {
    private final String name;
    private final double allowance;
    public static double balance;
    private final LocalDate nextRefill;

    Profile(String name, double allowance, double balance, LocalDate nextRefill) {
        this.name = name;
        this.allowance = allowance;
        this.balance = balance;
        this.nextRefill = nextRefill;
    }

    String name() { return name; }
    double allowance() { return allowance; }
    double balance() { return balance; }
    LocalDate nextRefill() { return nextRefill; }
}
