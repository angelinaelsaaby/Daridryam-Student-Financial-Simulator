import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Predicate;

/** All the "prediction" logic: health score, alerts, what-if, goals and Future Me. */
final class Engine {
    static final String[] CATEGORIES = {"Food", "Food Delivery", "Transport", "Education",
            "Entertainment", "Shopping", "Subscriptions", "Health", "Other"};
    /** Discretionary ("wants") categories - the ones Future Me plays with. */
    static final Set<String> WANTS = new HashSet<>(Arrays.asList(
            "Food Delivery", "Entertainment", "Shopping", "Subscriptions"));

    enum Level { OK, WARN, CRITICAL }

    static final class Alert {
        private final Level level;
        private final String title, message;
        Alert(Level level, String title, String message) { this.level = level; this.title = title; this.message = message; }
        Level level() { return level; }
        String title() { return title; }
        String message() { return message; }
    }

    static final class Snapshot {
        private final double balance, avgDaily, projected, runwayDays, safeDaily;
        private final int daysLeft, score;
        private final String label;
        Snapshot(double balance, double avgDaily, int daysLeft, double projected, double runwayDays,
                 double safeDaily, int score, String label) {
            this.balance = balance; this.avgDaily = avgDaily; this.daysLeft = daysLeft; this.projected = projected;
            this.runwayDays = runwayDays; this.safeDaily = safeDaily; this.score = score; this.label = label;
        }
        double balance() { return balance; }
        double avgDaily() { return avgDaily; }
        int daysLeft() { return daysLeft; }
        double projected() { return projected; }
        double runwayDays() { return runwayDays; }
        double safeDaily() { return safeDaily; }
        int score() { return score; }
        String label() { return label; }
    }

    static final class SimResult {
        private final double[] base, scen;
        private final int firstNegBase, firstNegScen;
        SimResult(double[] base, double[] scen, int firstNegBase, int firstNegScen) {
            this.base = base; this.scen = scen; this.firstNegBase = firstNegBase; this.firstNegScen = firstNegScen;
        }
        double[] base() { return base; }
        double[] scen() { return scen; }
        int firstNegBase() { return firstNegBase; }
        int firstNegScen() { return firstNegScen; }
    }

    static final class Habit {
        private final String name;
        private final double monthlySpend;
        private final double[] balances;
        Habit(String name, double monthlySpend, double[] balances) {
            this.name = name; this.monthlySpend = monthlySpend; this.balances = balances;
        }
        String name() { return name; }
        double monthlySpend() { return monthlySpend; }
        double[] balances() { return balances; }
    }

    static final class GoalPrediction {
        private final String text;
        private final boolean onTrack;
        GoalPrediction(String text, boolean onTrack) { this.text = text; this.onTrack = onTrack; }
        String text() { return text; }
        boolean onTrack() { return onTrack; }
    }

    private Engine() {}

    static String money(double v) {
        return String.format("%s\u20B9%,.0f", v < 0 ? "-" : "", Math.abs(v));
    }

    // ---------- basics ----------
    static int daysLeft(Profile p) {
        return (int) Math.max(1, ChronoUnit.DAYS.between(LocalDate.now(), p.nextRefill()));
    }

    /** Number of days the spending history actually covers (7..30). */
    static int windowDays(List<Tx> txs) {
        LocalDate today = LocalDate.now(), from = today.minusDays(29), first = null;
        for (Tx t : txs) {
            if (t.income() || t.date().isBefore(from)) continue;
            if (first == null || t.date().isBefore(first)) first = t.date();
        }
        if (first == null) return 30;
        long d = ChronoUnit.DAYS.between(first, today) + 1;
        return (int) Math.max(7, Math.min(30, d));
    }

    static double windowSpend(List<Tx> txs, Predicate<Tx> filter) {
        LocalDate from = LocalDate.now().minusDays(29);
        double sum = 0;
        for (Tx t : txs) {
            if (!t.income() && !t.date().isBefore(from) && filter.test(t)) sum += t.amount();
        }
        return sum;
    }

    static double avgDaily(List<Tx> txs) {
        return windowSpend(txs, t -> true) / windowDays(txs);
    }

    static Map<String, Double> byCategory(List<Tx> txs) {
        Map<String, Double> m = new HashMap<>();
        LocalDate from = LocalDate.now().minusDays(29);
        for (Tx t : txs) {
            if (!t.income() && !t.date().isBefore(from)) m.merge(t.category(), t.amount(), Double::sum);
        }
        LinkedHashMap<String, Double> sorted = new LinkedHashMap<>();
        m.entrySet().stream().sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .forEach(e -> sorted.put(e.getKey(), e.getValue()));
        return sorted;
    }

    /** Spending per day for the last n days; the last element is today. */
    static double[] dailySeries(List<Tx> txs, int n) {
        double[] out = new double[n];
        LocalDate today = LocalDate.now();
        for (Tx t : txs) {
            if (t.income()) continue;
            long ago = ChronoUnit.DAYS.between(t.date(), today);
            if (ago >= 0 && ago < n) out[n - 1 - (int) ago] += t.amount();
        }
        return out;
    }

    static double wantsShare(List<Tx> txs) {
        double total = windowSpend(txs, t -> true);
        return total <= 0 ? 0 : windowSpend(txs, t -> WANTS.contains(t.category())) / total;
    }

    /** What you could save per day if the allowance is spread over 30 days. */
    static double dailySavings(Profile p, List<Tx> txs) {
        return Math.max(0, p.allowance() / 30.0 - avgDaily(txs));
    }

    // ---------- financial health ----------
    static Snapshot snapshot(Profile p, List<Tx> txs, List<Goal> goals) {
        double avg = avgDaily(txs);
        int dl = daysLeft(p);
        double bal = p.balance();
        double projected = bal - avg * dl;
        double runway = avg <= 0 ? 999 : Math.max(0, bal / avg);
        double safe = bal / dl;

        double ratio = bal <= 0 ? 0 : (avg <= 0 ? 2 : runway / dl);
        double runwayPts = Math.min(ratio, 1.5) / 1.5 * 60;
        double goalPts = 10;
        if (!goals.isEmpty()) {
            double sum = 0;
            for (Goal g : goals) sum += Math.min(1, g.saved() / Math.max(1, g.target()));
            goalPts = sum / goals.size() * 20;
        }
        double wantsPts = (1 - Math.min(wantsShare(txs) / 0.5, 1)) * 20;
        int score = (int) Math.round(Math.max(0, Math.min(100, runwayPts + goalPts + wantsPts)));
        String label = score >= 75 ? "Healthy" : score >= 50 ? "Okay" : score >= 30 ? "At risk" : "Critical";
        return new Snapshot(bal, avg, dl, projected, runway, safe, score, label);
    }

    // ---------- Daridryam Alert ----------
    static Alert check(Profile p, List<Tx> txs, double amount) {
        double bal = p.balance(), avg = avgDaily(txs), after = bal - amount;
        int dl = daysLeft(p);
        if (after < 0) {
            return new Alert(Level.CRITICAL, "You can't afford this",
                    "This purchase is " + money(amount) + " but your balance is only " + money(bal)
                            + ". You'd be short by " + money(-after) + ".");
        }
        double runwayAfter = avg <= 0 ? 999 : after / avg;
        double safeAfter = after / dl;
        if (runwayAfter < dl) {
            double short_ = dl - runwayAfter;
            LocalDate broke = LocalDate.now().plusDays((long) runwayAfter);
            String msg = String.format("After spending %s you'd have %s. At your usual %s/day you run out around %s - "
                            + "about %.0f day(s) before your next refill. Your safe budget would drop to %s/day.",
                    money(amount), money(after), money(avg), broke, Math.ceil(short_), money(safeAfter));
            Level lv = runwayAfter < dl * 0.5 ? Level.CRITICAL : Level.WARN;
            return new Alert(lv, lv == Level.CRITICAL ? "Daridryam Alert: this will wreck your month"
                    : "Daridryam Alert: this will leave you short", msg);
        }
        if (amount > bal * 0.4 || safeAfter < avg * 1.15) {
            return new Alert(Level.WARN, "Heads up: it gets tight",
                    String.format("You can afford it, but your cushion shrinks to %s/day of safe spending "
                            + "(you normally spend %s/day).", money(safeAfter), money(avg)));
        }
        return new Alert(Level.OK, "You're fine",
                String.format("After this you'd still have %s, about %s/day until refill (you spend ~%s/day).",
                        money(after), money(safeAfter), money(avg)));
    }

    // ---------- What-If Simulator ----------
    /**
     * Day-by-day balance for the baseline (current pace) and the scenario.
     * oneTime = one-off spend today; dailyDelta = change in daily spending (negative = save more).
     */
    static SimResult simulate(Profile p, List<Tx> txs, double oneTime, double dailyDelta, int horizon) {
        double avg = avgDaily(txs);
        double[] base = new double[horizon + 1], scen = new double[horizon + 1];
        base[0] = p.balance();
        scen[0] = p.balance() - oneTime;
        LocalDate today = LocalDate.now(), nr = p.nextRefill();
        while (!nr.isAfter(today)) nr = nr.plusMonths(1);
        int fb = -1, fs = scen[0] < 0 ? 0 : -1;
        for (int d = 1; d <= horizon; d++) {
            LocalDate date = today.plusDays(d);
            double refill = 0;
            if (date.equals(nr)) {
                refill = p.allowance();
                nr = nr.plusMonths(1);
            }
            base[d] = base[d - 1] - avg + refill;
            scen[d] = scen[d - 1] - Math.max(0, avg + dailyDelta) + refill;
            if (fb < 0 && base[d] < 0) fb = d;
            if (fs < 0 && scen[d] < 0) fs = d;
        }
        return new SimResult(base, scen, fb, fs);
    }

    // ---------- Goals ----------
    static GoalPrediction predictGoal(Goal g, double dailySave) {
        double remaining = g.target() - g.saved();
        if (remaining <= 0) return new GoalPrediction("Goal reached!", true);
        long daysToDeadline = ChronoUnit.DAYS.between(LocalDate.now(), g.deadline());
        double required = daysToDeadline > 0 ? remaining / daysToDeadline : remaining;
        if (dailySave <= 0) {
            return new GoalPrediction("Not reachable at current spending - need " + money(required) + "/day", false);
        }
        long days = (long) Math.ceil(remaining / dailySave);
        LocalDate eta = LocalDate.now().plusDays(days);
        boolean ok = !eta.isAfter(g.deadline());
        return new GoalPrediction("ETA " + eta + " (" + days + " days) - "
                + (ok ? "on track" : "behind, need " + money(required) + "/day"), ok);
    }

    // ---------- Future Me ----------
    static List<Habit> futureMe(Profile p, List<Tx> txs, int months) {
        int wd = windowDays(txs);
        double wants = windowSpend(txs, t -> WANTS.contains(t.category())) / wd * 30;
        double needs = windowSpend(txs, t -> !WANTS.contains(t.category())) / wd * 30;
        String[] names = {"Frugal Me", "Current Me", "Spender Me"};
        double[][] mult = {{0.6, 1.0}, {1.0, 1.0}, {1.4, 1.05}}; // {wants, needs}
        List<Habit> out = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            double monthly = wants * mult[i][0] + needs * mult[i][1];
            double[] bal = new double[months + 1];
            bal[0] = p.balance();
            for (int m = 1; m <= months; m++) bal[m] = bal[m - 1] + p.allowance() - monthly;
            out.add(new Habit(names[i], monthly, bal));
        }
        return out;
    }
}
