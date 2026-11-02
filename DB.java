import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** All MySQL access through plain JDBC. Edit HOST / PORT / DATABASE below if needed. */
final class DB {
    private static final String HOST = "localhost";
    private static final String PORT = "3306";
    private static final String DATABASE = "daridryam";
    private static final String OPTS = "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private static String user = "root", pass = "";

    private DB() {}

    private static Connection conn() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE + OPTS, user, pass);
    }

    private static IllegalStateException fail(SQLException e) {
        return new IllegalStateException("Database error: " + e.getMessage(), e);
    }

    /** Connects, then creates the database, tables and the single profile row if missing. */
    static void connect(String u, String p) throws SQLException {
        user = u;
        pass = p;
        try (Connection c = DriverManager.getConnection("jdbc:mysql://" + HOST + ":" + PORT + "/" + OPTS, user, pass);
             Statement s = c.createStatement()) {
            s.execute("CREATE DATABASE IF NOT EXISTS `" + DATABASE + "`");
        }
        try (Connection c = conn(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS profile (id INT PRIMARY KEY, name VARCHAR(60) NOT NULL, "
                    + "allowance DECIMAL(12,2) NOT NULL, balance DECIMAL(12,2) NOT NULL, next_refill DATE NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS transactions (id INT AUTO_INCREMENT PRIMARY KEY, tx_date DATE NOT NULL, "
                    + "category VARCHAR(40) NOT NULL, description VARCHAR(120), amount DECIMAL(12,2) NOT NULL, "
                    + "is_income BOOLEAN NOT NULL DEFAULT FALSE)");
            s.execute("CREATE TABLE IF NOT EXISTS goals (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(80) NOT NULL, "
                    + "target DECIMAL(12,2) NOT NULL, saved DECIMAL(12,2) NOT NULL DEFAULT 0, deadline DATE NOT NULL)");
        }
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement("INSERT IGNORE INTO profile VALUES (1,?,?,?,?)")) {
            ps.setString(1, "Student");
            ps.setDouble(2, 10000);
            ps.setDouble(3, 10000);
            ps.setDate(4, java.sql.Date.valueOf(LocalDate.now().withDayOfMonth(1).plusMonths(1)));
            ps.executeUpdate();
        }
    }

    // ---------- profile ----------
    static Profile getProfile() {
        try (Connection c = conn(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT name, allowance, balance, next_refill FROM profile WHERE id=1")) {
            r.next();
            return new Profile(r.getString(1), r.getDouble(2), r.getDouble(3), r.getDate(4).toLocalDate());
        } catch (SQLException e) { throw fail(e); }
    }

    static void saveProfile(Profile p) {
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE profile SET name=?, allowance=?, balance=?, next_refill=? WHERE id=1")) {
            ps.setString(1, p.name());
            ps.setDouble(2, p.allowance());
            ps.setDouble(3, p.balance());
            ps.setDate(4, java.sql.Date.valueOf(p.nextRefill()));
            ps.executeUpdate();
        } catch (SQLException e) { throw fail(e); }
    }

    // ---------- transactions ----------
    static List<Tx> listTx() {
        List<Tx> out = new ArrayList<>();
        try (Connection c = conn(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT id, tx_date, category, description, amount, is_income "
                     + "FROM transactions ORDER BY tx_date DESC, id DESC")) {
            while (r.next()) {
                out.add(new Tx(r.getInt(1), r.getDate(2).toLocalDate(), r.getString(3),
                        r.getString(4) == null ? "" : r.getString(4), r.getDouble(5), r.getBoolean(6)));
            }
        } catch (SQLException e) { throw fail(e); }
        return out;
    }

    /** Inserts the transaction and adjusts the balance in one SQL transaction. */
    static void addTx(Tx t) {
        try (Connection c = conn()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO transactions(tx_date,category,description,amount,is_income) VALUES (?,?,?,?,?)")) {
                ps.setDate(1, java.sql.Date.valueOf(t.date()));
                ps.setString(2, t.category());
                ps.setString(3, t.description());
                ps.setDouble(4, t.amount());
                ps.setBoolean(5, t.income());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("UPDATE profile SET balance = balance + ? WHERE id=1")) {
                ps.setDouble(1, t.income() ? t.amount() : -t.amount());
                ps.executeUpdate();
            }
            c.commit();
        } catch (SQLException e) { throw fail(e); }
    }

    /** Deletes the transaction and reverses its effect on the balance. */
    static void deleteTx(Tx t) {
        try (Connection c = conn()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM transactions WHERE id=?")) {
                ps.setInt(1, t.id());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("UPDATE profile SET balance = balance + ? WHERE id=1")) {
                ps.setDouble(1, t.income() ? -t.amount() : t.amount());
                ps.executeUpdate();
            }
            c.commit();
        } catch (SQLException e) { throw fail(e); }
    }

    // ---------- goals ----------
    static List<Goal> listGoals() {
        List<Goal> out = new ArrayList<>();
        try (Connection c = conn(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT id, name, target, saved, deadline FROM goals ORDER BY deadline")) {
            while (r.next()) {
                out.add(new Goal(r.getInt(1), r.getString(2), r.getDouble(3), r.getDouble(4), r.getDate(5).toLocalDate()));
            }
        } catch (SQLException e) { throw fail(e); }
        return out;
    }

    static void addGoal(Goal g) {
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement("INSERT INTO goals(name,target,saved,deadline) VALUES (?,?,?,?)")) {
            ps.setString(1, g.name());
            ps.setDouble(2, g.target());
            ps.setDouble(3, g.saved());
            ps.setDate(4, java.sql.Date.valueOf(g.deadline()));
            ps.executeUpdate();
        } catch (SQLException e) { throw fail(e); }
    }

    static void setGoalSaved(int id, double saved) {
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement("UPDATE goals SET saved=? WHERE id=?")) {
            ps.setDouble(1, Math.max(0, saved));
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) { throw fail(e); }
    }

    static void deleteGoal(int id) {
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement("DELETE FROM goals WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { throw fail(e); }
    }

    // ---------- bulk ----------
    static void clearAll() {
        try (Connection c = conn(); Statement s = c.createStatement()) {
            s.executeUpdate("DELETE FROM transactions");
            s.executeUpdate("DELETE FROM goals");
        } catch (SQLException e) { throw fail(e); }
    }

    /** Replaces everything with 30 days of realistic student spending so the app can be demoed. */
    static void seedDemo() {
        clearAll();
        Random rnd = new Random(42);
        LocalDate today = LocalDate.now();
        List<Tx> list = new ArrayList<>();
        for (int off = 29; off >= 0; off--) {
            LocalDate d = today.minusDays(off);
            boolean weekend = d.getDayOfWeek().getValue() >= 6;
            list.add(new Tx(0, d, "Food", "Mess / canteen", 60 + rnd.nextInt(60), false));
            if (rnd.nextInt(10) < 7) list.add(new Tx(0, d, "Transport", "Bus / auto", 20 + rnd.nextInt(40), false));
            if (weekend && rnd.nextInt(10) < 6) list.add(new Tx(0, d, "Food Delivery", "Zomato / Swiggy", 150 + rnd.nextInt(200), false));
            if (weekend && rnd.nextInt(10) < 3) list.add(new Tx(0, d, "Entertainment", "Movie / games", 150 + rnd.nextInt(250), false));
            if (off == 25) list.add(new Tx(0, d, "Education", "Books & notes", 450, false));
            if (off == 20) list.add(new Tx(0, d, "Subscriptions", "Streaming", 199, false));
            if (off == 18) list.add(new Tx(0, d, "Subscriptions", "Music", 119, false));
            if (off == 14) list.add(new Tx(0, d, "Health", "Pharmacy", 220, false));
            if (off == 9) list.add(new Tx(0, d, "Shopping", "Sneakers", 1299, false));
            if (off == 3) list.add(new Tx(0, d, "Shopping", "T-shirt", 599, false));
        }
        double total = list.stream().mapToDouble(Tx::amount).sum();
        saveProfile(new Profile("Student", 8000, total + 2000, today.plusDays(12)));
        for (Tx t : list) addTx(t);
        addGoal(new Goal(0, "New headphones", 3000, 600, today.plusMonths(2)));
        addGoal(new Goal(0, "Trip with friends", 8000, 1500, today.plusMonths(4)));
    }
}
