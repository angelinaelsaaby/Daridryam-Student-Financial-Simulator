import java.time.LocalDate;

final class Goal {
    private final int id;
    private final String name;
    private final double target;
    private final double saved;
    private final LocalDate deadline;

    Goal(int id, String name, double target, double saved, LocalDate deadline) {
        this.id = id;
        this.name = name;
        this.target = target;
        this.saved = saved;
        this.deadline = deadline;
    }

    int id() { return id; }
    String name() { return name; }
    double target() { return target; }
    double saved() { return saved; }
    LocalDate deadline() { return deadline; }
}
