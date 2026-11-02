
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
/**
 * Daridryam - Student Financial Survival Simulator (simple version).
 * Swing: JFrame, JPanel, JLabel, JButton, JTextField, JCheckBox, JOptionPane.
 * AWT: CardLayout / layouts, ActionListener, actionPerformed, getActionCommand.
 * Data: MySQL through JDBC (see DB.java).
 */
public class Main extends JFrame implements ActionListener {
    private Profile profile;
    private List<Tx> txs = new ArrayList<>();
    private List<Goal> goals = new ArrayList<>();

    private final CardLayout cards = new CardLayout();
    private final JPanel body = new JPanel(cards);

    // dashboard
    private final JLabel dashInfo = new JLabel();
    private final JTextField chkAmt = new JTextField(8);
    private final JLabel chkResult = new JLabel();

    // transactions
    private final JTextField txDate = new JTextField(LocalDate.now().toString(), 9);
    private final JTextField txCat = new JTextField("Food", 12);
    private final JTextField txNote = new JTextField(14);
    private final JTextField txAmt = new JTextField(8);
    private final JCheckBox txIncome = new JCheckBox("Income");
    private final JTextField txDelId = new JTextField(5);
    private final JLabel txList = new JLabel();

    // what-if
    private final JTextField wOne = new JTextField("0", 7);
    double bal=Profile.balance;
    private final JLabel Currbal = new JLabel(String.valueOf(bal));
    private final JLabel wResult = new JLabel();

    // goals
    private final JTextField gName = new JTextField(12);
    private final JTextField gTarget = new JTextField(7);
    private final JTextField gSaved = new JTextField("0", 6);
    private final JTextField gDeadline = new JTextField(LocalDate.now().plusMonths(3).toString(), 9);
    private final JTextField gId = new JTextField(4);
    private final JTextField gAdd = new JTextField(7);
    private final JLabel gList = new JLabel();

    // analysis + future me
    private final JLabel analysis = new JLabel();
    private final JLabel future = new JLabel();

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            if (!connectDatabase()) System.exit(0);
            new Main().setVisible(true);
        });
    }

    /** Asks for MySQL credentials with JOptionPane until the connection works (or the user cancels). */
    private static boolean connectDatabase() {
        String user = "root";
        while (true) {
            user = JOptionPane.showInputDialog(null, "MySQL user:", user);
            if (user == null) return false;
            String pass = JOptionPane.showInputDialog(null, "MySQL password (leave empty if none):");
            if (pass == null) return false;
            try {
                DB.connect(user.trim(), pass);
                return true;
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Could not connect to MySQL:\n" + e.getMessage()
                        + "\n\nMake sure MySQL is running and the Connector/J jar is on the classpath.",
                        "Daridryam", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public Main() {
        super("Daridryam - Student Financial Survival Simulator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 620);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        for (JLabel l : new JLabel[]{dashInfo, chkResult, txList, wResult, gList, analysis, future}) {
            l.setVerticalAlignment(JLabel.TOP);
        }

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        nav.add(button("Dashboard", "nav:Dashboard"));
        nav.add(button("Transactions", "nav:Transactions"));
        nav.add(button("What-If", "nav:What-If"));
        nav.add(button("Goals", "nav:Goals"));
        nav.add(button("Analysis", "nav:Analysis"));
        nav.add(button("Future Me", "nav:Future Me"));
        nav.add(button("Profile", "profile"));
        nav.add(button("Demo data", "demo"));
        nav.add(button("Clear data", "clear"));
        add(nav, BorderLayout.NORTH);

        body.add(buildDashboard(), "Dashboard");
        body.add(buildTransactions(), "Transactions");
        body.add(buildWhatIf(), "What-If");
        body.add(buildGoals(), "Goals");
        body.add(analysis, "Analysis");
        body.add(future, "Future Me");
        add(body, BorderLayout.CENTER);
        reload();
    }

    // ------------------------------------------------------------ helpers
    private JButton button(String text, String command) {
        JButton b = new JButton(text);
        b.setActionCommand(command);
        b.addActionListener(this);
        return b;
    }

    private static JPanel flow(Component... cs) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        for (Component c : cs) p.add(c);
        return p;
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Shows simple HTML text in a label. */
    private static void show(JLabel l, String html) {
        l.setText("<html><body style='margin:10px'>" + html + "</body></html>");
    }

    private static String color(String text, String name) {
        return "<font color='" + name + "'>" + text + "</font>";
    }

    private static String levelColor(Engine.Level l) {
        return l == Engine.Level.OK ? "green" : l == Engine.Level.WARN ? "#CC7700" : "red";
    }

    private boolean confirm(String msg) {
        return JOptionPane.showConfirmDialog(this, msg, "Daridryam", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Daridryam", JOptionPane.WARNING_MESSAGE);
    }

    private static String wrap(String s) {
        StringBuilder out = new StringBuilder();
        int col = 0;
        for (String w : s.split(" ")) {
            if (col + w.length() > 70) {
                out.append('\n');
                col = 0;
            }
            out.append(w).append(' ');
            col += w.length() + 1;
        }
        return out.toString().trim();
    }

    // ------------------------------------------------------------ pages
    private JPanel buildDashboard() {
        JPanel p = new JPanel(new BorderLayout());
        chkAmt.setActionCommand("check");
        chkAmt.addActionListener(this);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(flow(new JLabel("Can I afford it? Purchase amount \u20B9"), chkAmt, button("Check", "check")),
                BorderLayout.NORTH);
        bottom.add(chkResult, BorderLayout.CENTER);
        p.add(dashInfo, BorderLayout.CENTER);
        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildTransactions() {
        JPanel p = new JPanel(new BorderLayout());
        txAmt.setActionCommand("addTx");
        txAmt.addActionListener(this);
        JPanel form = new JPanel(new GridLayout(3, 1));
        form.add(flow(new JLabel("Date"), txDate, new JLabel("Category"), txCat, new JLabel("Note"), txNote,
                new JLabel("\u20B9"), txAmt, txIncome, button("Add", "addTx")));
        form.add(new JLabel("   Categories: " + String.join(", ", Engine.CATEGORIES) + " (anything else = Other)"));
        form.add(flow(new JLabel("Delete transaction #"), txDelId, button("Delete", "delTx")));
        p.add(form, BorderLayout.NORTH);
        p.add(txList, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildWhatIf() {
        JPanel p = new JPanel(new BorderLayout());
        JPanel top = new JPanel(new GridLayout(2, 1));
        top.add(flow(new JLabel("What-if amount \u20B9"), wOne, new JLabel("Current Balance \u20B9"),Currbal,button("Simulate", "simulate")));
        p.add(top, BorderLayout.NORTH);
        p.add(wResult, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildGoals() {
        JPanel p = new JPanel(new BorderLayout());
        JPanel top = new JPanel(new GridLayout(2, 1));
        top.add(flow(new JLabel("Goal"), gName, new JLabel("Target \u20B9"), gTarget, new JLabel("Saved \u20B9"), gSaved,
                new JLabel("Deadline"), gDeadline, button("Add goal", "addGoal")));
        top.add(flow(new JLabel("Goal #"), gId, new JLabel("Set aside \u20B9"), gAdd,
                button("Add savings", "addSaving"), button("Delete goal", "delGoal")));
        p.add(top, BorderLayout.NORTH);
        p.add(gList, BorderLayout.CENTER);
        return p;
    }

    // ------------------------------------------------------------ events
    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        try {
            if (cmd.startsWith("nav:")) {
                cards.show(body, cmd.substring(4));
            } else if (cmd.equals("check")) {
                checkPurchase();
            } else if (cmd.equals("addTx")) {
                addTransaction();
            } else if (cmd.equals("delTx")) {
                deleteTransaction();
            } else if (cmd.equals("simulate")) {
                simulate();
            } else if (cmd.equals("preset1")) {
                preset("2000");
            } else if (cmd.equals("preset2")) {
                preset("0");
            } else if (cmd.equals("preset3")) {
                preset("0");
            } else if (cmd.equals("preset4")) {
                preset("500");
            } else if (cmd.equals("addGoal")) {
                addGoal();
            } else if (cmd.equals("addSaving")) {
                addSaving();
            } else if (cmd.equals("delGoal")) {
                deleteGoal();
            } else if (cmd.equals("profile")) {
                editProfile();
            } else if (cmd.equals("demo")) {
                if (confirm("Replace all transactions and goals with demo data?")) {
                    DB.seedDemo();
                    reload();
                }
            } else if (cmd.equals("clear")) {
                if (confirm("Delete ALL transactions and goals?")) {
                    DB.clearAll();
                    reload();
                }
            }
        } catch (RuntimeException ex) {
            warn(ex.getMessage() == null ? ex.toString() : ex.getMessage());
        }
    }

    private void checkPurchase() {
        double a;
        try {
            a = Double.parseDouble(chkAmt.getText().trim());
            if (a <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            warn("Enter a positive amount.");
            return;
        }
        Engine.Alert al = Engine.check(profile, txs, a);
        show(chkResult, color("<b>" + esc(al.title()) + "</b><br>" + esc(al.message()), levelColor(al.level())));
    }

    private void addTransaction() {
        LocalDate d;
        double a;
        try {
            d = LocalDate.parse(txDate.getText().trim());
            a = Double.parseDouble(txAmt.getText().trim());
            if (a <= 0) throw new NumberFormatException();
        } catch (Exception ex) {
            warn("Enter a date like 2026-10-23 and a positive amount.");
            return;
        }
        boolean income = txIncome.isSelected();
        String cat = "Other";
        for (String c : Engine.CATEGORIES) if (c.equalsIgnoreCase(txCat.getText().trim())) cat = c;
        if (!income) {
            Engine.Alert al = Engine.check(profile, txs, a);
            if (al.level() != Engine.Level.OK) {
                int r = JOptionPane.showConfirmDialog(this, al.title() + "\n\n" + wrap(al.message()) + "\n\nSpend anyway?",
                        "Daridryam Alert", JOptionPane.YES_NO_OPTION,
                        al.level() == Engine.Level.CRITICAL ? JOptionPane.ERROR_MESSAGE : JOptionPane.WARNING_MESSAGE);
                if (r != JOptionPane.YES_OPTION) return;
            }
        }
        DB.addTx(new Tx(0, d, income ? "Other" : cat, txNote.getText().trim(), a, income));
        txNote.setText("");
        txAmt.setText("");
        reload();
    }

    private void deleteTransaction() {
        int id;
        try {
            id = Integer.parseInt(txDelId.getText().trim());
        } catch (NumberFormatException ex) {
            warn("Type the transaction number (#) shown in the list.");
            return;
        }
        for (Tx t : txs) {
            if (t.id() == id) {
                DB.deleteTx(t);
                txDelId.setText("");
                reload();
                return;
            }
        }
        warn("No transaction with number " + id + ".");
    }

    private void preset(String one) {
        wOne.setText(one);
        simulate();
    }

    private void simulate() {

    double amount = Double.parseDouble(wOne.getText());

    double balance = bal - amount;

    show(wResult, "Remaining balance: " + balance);
}
    private void addGoal() {
        try {
            String n = gName.getText().trim();
            if (n.isEmpty()) throw new IllegalArgumentException();
            DB.addGoal(new Goal(0, n, Double.parseDouble(gTarget.getText().trim()),
                    Double.parseDouble(gSaved.getText().trim()), LocalDate.parse(gDeadline.getText().trim())));
            gName.setText("");
            gTarget.setText("");
            reload();
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            warn("Enter a goal name, a target amount and a deadline like 2027-01-15.");
        }
    }

    private Goal findGoal() {
        try {
            int id = Integer.parseInt(gId.getText().trim());
            for (Goal g : goals) if (g.id() == id) return g;
        } catch (NumberFormatException ignored) { }
        warn("Type a goal number (#) from the list.");
        return null;
    }

    private void addSaving() {
        Goal g = findGoal();
        if (g == null) return;
        try {
            DB.setGoalSaved(g.id(), g.saved() + Double.parseDouble(gAdd.getText().trim()));
            gAdd.setText("");
            reload();
        } catch (NumberFormatException ex) {
            warn("Enter how much you set aside.");
        }
    }

    private void deleteGoal() {
        Goal g = findGoal();
        if (g == null) return;
        DB.deleteGoal(g.id());
        gId.setText("");
        reload();
    }

    private void editProfile() {
        JTextField name = new JTextField(profile.name(), 14);
        JTextField allowance = new JTextField(String.valueOf(profile.allowance()), 14);
        JTextField balance = new JTextField(String.valueOf(profile.balance()), 14);
        JTextField refill = new JTextField(profile.nextRefill().toString(), 14);
        JPanel p = new JPanel(new GridLayout(0, 2, 8, 8));
        p.add(new JLabel("Name"));
        p.add(name);
        p.add(new JLabel("Monthly allowance"));
        p.add(allowance);
        p.add(new JLabel("Current balance"));
        p.add(balance);
        p.add(new JLabel("Next refill (yyyy-mm-dd)"));
        p.add(refill);
        if (JOptionPane.showConfirmDialog(this, p, "Profile", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            DB.saveProfile(new Profile(name.getText().trim().isEmpty() ? "Student" : name.getText().trim(),
                    Double.parseDouble(allowance.getText().trim()), Double.parseDouble(balance.getText().trim()),
                    LocalDate.parse(refill.getText().trim())));
            reload();
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            warn("Please enter valid numbers and a date like 2026-11-01.");
        }
    }

    // ------------------------------------------------------------ refresh
    private void reload() {
        profile = DB.getProfile();
        Currbal.setText(String.valueOf(profile.balance()));
        txs = DB.listTx();
        goals = DB.listGoals();
        refreshDashboard();
        refreshTransactions();
        simulate();
        refreshGoals();
        refreshAnalysis();
        refreshFuture();
        if (chkResult.getText() == null || chkResult.getText().isEmpty()) {
            show(chkResult, color("Type an amount to see what a purchase would do to your month.", "gray"));
        }
    }

    private void refreshDashboard() {
        Engine.Snapshot s = Engine.snapshot(profile, txs, goals);
        StringBuilder sb = new StringBuilder("<h2>Hello, " + esc(profile.name()) + "</h2>");
        sb.append("<b>Financial health: ").append(s.score()).append(" / 100 (").append(s.label()).append(")</b><br><br>");
        sb.append("Balance: <b>").append(Engine.money(s.balance())).append("</b> (allowance ")
                .append(Engine.money(profile.allowance())).append("/month)<br>");
        sb.append("Next refill: ").append(profile.nextRefill()).append(" (").append(s.daysLeft()).append(" days)<br>");
        sb.append("Average daily spend: ").append(Engine.money(s.avgDaily())).append(" (last ")
                .append(Engine.windowDays(txs)).append(" days)<br>");
        sb.append("Safe to spend per day: ").append(Engine.money(s.safeDaily())).append("<br>");
        sb.append("Money lasts: ").append(s.runwayDays() >= 999 ? "no spending yet" : String.format("%.0f days", s.runwayDays()))
                .append("<br>");
        sb.append("Projected balance at refill: ")
                .append(color("<b>" + Engine.money(s.projected()) + "</b>", s.projected() < 0 ? "red" : "green")).append("<br><br>");
        if (txs.isEmpty()) {
            sb.append("No transactions yet. Add some, or press Demo data.");
        } else if (s.projected() < 0) {
            sb.append(color(String.format("At this pace you run out in about %.0f days, before your refill. "
                            + "Keep spending under %s/day to make it.", s.runwayDays(), Engine.money(s.safeDaily())), "red"));
        } else {
            sb.append(color("You're on track. You'll have about " + Engine.money(s.projected()) + " left at refill.", "green"));
        }
        show(dashInfo, sb.toString());
    }

    private void refreshTransactions() {
        StringBuilder sb = new StringBuilder("<b>Recent transactions (newest first)</b><br><br>");
        if (txs.isEmpty()) sb.append("Nothing yet.");
        int shown = 0;
        for (Tx t : txs) {
            if (shown++ >= 18) {
                sb.append("... and ").append(txs.size() - 18).append(" older ones");
                break;
            }
            sb.append(color(String.format("#%d &nbsp; %s &nbsp; %s &nbsp; %s &nbsp; <b>%s%s</b>", t.id(), t.date(),
                    esc(t.category()), esc(t.description()), t.income() ? "+ " : "- ", Engine.money(t.amount())),
                    t.income() ? "green" : "black")).append("<br>");
        }
        show(txList, sb.toString());
    }

    private void refreshGoals() {
        double ds = Engine.dailySavings(profile, txs);
        StringBuilder sb = new StringBuilder("<b>Your goals</b><br>");
        sb.append(ds > 0 ? "You can save about " + Engine.money(ds) + " per day at your current pace."
                : color("You're spending your whole allowance - nothing is left over for goals.", "red")).append("<br><br>");
        if (goals.isEmpty()) sb.append("No goals yet - add one above.");
        for (Goal g : goals) {
            int pct = (int) Math.min(100, Math.round(100 * g.saved() / Math.max(1, g.target())));
            Engine.GoalPrediction gp = Engine.predictGoal(g, ds);
            sb.append(String.format("#%d &nbsp; <b>%s</b> &nbsp; %s of %s (%d%%) &nbsp; deadline %s<br>", g.id(), esc(g.name()),
                    Engine.money(g.saved()), Engine.money(g.target()), pct, g.deadline()));
            sb.append(color(esc(gp.text()), gp.onTrack() ? "green" : "#CC7700")).append("<br><br>");
        }
        show(gList, sb.toString());
    }

    private void refreshAnalysis() {
        StringBuilder sb = new StringBuilder("<h3>Spending analysis (last 30 days)</h3>");
        Map<String, Double> cat = Engine.byCategory(txs);
        if (cat.isEmpty()) {
            sb.append("Nothing to analyse yet.");
        } else {
            double total = cat.values().stream().mapToDouble(Double::doubleValue).sum();
            sb.append("Total spent: <b>").append(Engine.money(total)).append("</b><br><br>");
            for (Map.Entry<String, Double> e : cat.entrySet()) {
                sb.append(String.format("%s: %s (%.0f%%)<br>", esc(e.getKey()), Engine.money(e.getValue()),
                        100 * e.getValue() / total));
            }
            sb.append(String.format("<br>Wants (delivery, shopping, fun, subscriptions) are %.0f%% of your spending.<br>",
                    Engine.wantsShare(txs) * 100));
            Tx big = null;
            LocalDate from = LocalDate.now().minusDays(29);
            for (Tx t : txs) {
                if (!t.income() && !t.date().isBefore(from) && (big == null || t.amount() > big.amount())) big = t;
            }
            if (big != null) {
                sb.append(String.format("Largest single expense: %s on %s (%s).", Engine.money(big.amount()), big.date(),
                        esc(big.category())));
            }
        }
        show(analysis, sb.toString());
    }

    private void refreshFuture() {
        StringBuilder sb = new StringBuilder("<h3>Future Me - your balance in 6 months</h3>");
        if (txs.isEmpty()) {
            sb.append("Add some spending history and Future Me will show where your habits lead.");
        } else {
            List<Engine.Habit> hs = Engine.futureMe(profile, txs, 6);
            for (Engine.Habit h : hs) {
                double end = h.balances()[6];
                sb.append(String.format("<b>%s</b>: spends %s/month, balance in 6 months = %s<br>", h.name(),
                        Engine.money(h.monthlySpend()), color("<b>" + Engine.money(end) + "</b>", end < 0 ? "red" : "green")));
            }
            double gap = hs.get(0).balances()[6] - hs.get(2).balances()[6];
            double now = hs.get(1).balances()[6];
            sb.append("<br>Frugal Me trims wants by 40%. Spender Me raises wants by 40% and needs by 5%.<br>");
            sb.append("The gap between Frugal Me and Spender Me after 6 months is ").append(Engine.money(gap)).append(".<br>");
            sb.append(now < 0 ? color("Keep today's habits and you'll be " + Engine.money(-now) + " short.", "red")
                    : "Keep today's habits and you'll be at " + Engine.money(now) + ".");
        }
        show(future, sb.toString());
    }
}
