import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Scanner;

public class DatabaseManager {

    // loads the connection string from the .env file
    private static final String URL = loadDbUrl();

    private static String loadDbUrl() {
        try {
            Scanner scanner = new Scanner(new File(".env"));
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.startsWith("DB_URL=")) {
                    scanner.close();
                    return line.substring(7);
                }
            }
            scanner.close();
        } catch (FileNotFoundException e) {
        }
        // fallback to system env variable
        return System.getenv("DB_URL");
    }

    public DatabaseManager() {
        if (URL == null || URL.isEmpty()) {
            System.out.println("[DatabaseManager] FATAL ERROR: DB_URL environment variable is not set.");
            System.out.println(
                    "Make sure you have a .env file in your root directory containing: DB_URL=jdbc:postgresql://...");
        }

        // load the jdbc driver
        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("[DatabaseManager] PostgreSQL JDBC Driver loaded successfully.");
        } catch (ClassNotFoundException e) {
            System.out.println("[DatabaseManager] ERROR: PostgreSQL JDBC Driver not found.");
            e.printStackTrace();
        }
    }

    // opens a new connection to the supabase db
    public Connection getConnection() {
        try {
            return DriverManager.getConnection(URL);
        } catch (SQLException e) {
            System.out.println("[DatabaseManager] ERROR: Failed to connect to the database.");
            System.out.println("Check your connection string, password, and internet connection.");
            System.out.println("Details: " + e.getMessage());
            return null;
        }
    }

    // inserts a new user with a hashed password into the users table
    public int registerUser(String fullName, String email, String plaintextPassword,
            String standing, String accessLevel, String adminRole) {

        // hash before storing
        String passwordHash = PasswordUtil.hashPassword(plaintextPassword);

        String sql = "INSERT INTO users (full_name, email, password_hash, standing, access_level, admin_role) "
                + "VALUES (?, ?, ?, ?::standing_enum, ?::access_level_enum, ?::admin_role_enum) "
                + "RETURNING user_id";

        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setString(3, passwordHash);
            ps.setString(4, standing != null ? standing : "GOOD");
            ps.setString(5, accessLevel != null ? accessLevel : "USER");

            if (adminRole != null) {
                ps.setString(6, adminRole);
            } else {
                ps.setNull(6, Types.OTHER);
            }

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int newId = rs.getInt("user_id");
                System.out.println("[DatabaseManager] User registered successfully. ID: " + newId);
                return newId;
            }

        } catch (SQLException e) {
            System.out.println("[DatabaseManager] ERROR: Failed to register user.");
            System.out.println("Details: " + e.getMessage());
        }

        return -1;
    }

    // inserts a new checking or savings account into the accounts table
    public int createAccount(int ownerId, BigDecimal initialBalance, String accountType,
            BigDecimal overdraftLimit, BigDecimal interestRate) {

        String sql = "INSERT INTO accounts (owner_id, balance, account_type, status, overdraft_limit, interest_rate) "
                + "VALUES (?, ?, ?::account_type_enum, 'ACTIVE'::account_status_enum, ?, ?) "
                + "RETURNING account_id";

        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, ownerId);
            ps.setBigDecimal(2, initialBalance != null ? initialBalance : BigDecimal.ZERO);
            ps.setString(3, accountType);

            if (overdraftLimit != null) {
                ps.setBigDecimal(4, overdraftLimit);
            } else {
                ps.setNull(4, Types.NUMERIC);
            }

            if (interestRate != null) {
                ps.setBigDecimal(5, interestRate);
            } else {
                ps.setNull(5, Types.NUMERIC);
            }

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int newId = rs.getInt("account_id");
                System.out.println("[DatabaseManager] Account created successfully. ID: " + newId);
                return newId;
            }

        } catch (SQLException e) {
            System.out.println("[DatabaseManager] ERROR: Failed to create account.");
            System.out.println("Details: " + e.getMessage());
        }

        return -1;
    }

    // looks up a user by id and prints their info
    public boolean getUserById(int userId) {

        String sql = "SELECT user_id, full_name, email, standing, access_level, admin_role, "
                + "created_at, updated_at "
                + "FROM users WHERE user_id = ?";

        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                System.out.println("──────────────── User Details ────────────────");
                System.out.println("  User ID      : " + rs.getInt("user_id"));
                System.out.println("  Full Name    : " + rs.getString("full_name"));
                System.out.println("  Email        : " + rs.getString("email"));
                System.out.println("  Standing     : " + rs.getString("standing"));
                System.out.println("  Access Level : " + rs.getString("access_level"));

                String adminRole = rs.getString("admin_role");
                if (adminRole != null) {
                    System.out.println("  Admin Role   : " + adminRole);
                }

                System.out.println("  Created At   : " + rs.getTimestamp("created_at"));
                System.out.println("  Updated At   : " + rs.getTimestamp("updated_at"));
                System.out.println("──────────────────────────────────────────────");
                return true;
            } else {
                System.out.println("[DatabaseManager] No user found with ID: " + userId);
            }

        } catch (SQLException e) {
            System.out.println("[DatabaseManager] ERROR: Failed to fetch user.");
            System.out.println("Details: " + e.getMessage());
        }

        return false;
    }

    // gets all accounts for a specific user
    public int getAccountsByUser(int ownerId) {

        String sql = "SELECT account_id, owner_id, balance, account_type, status, "
                + "overdraft_limit, interest_rate, created_at, updated_at "
                + "FROM accounts WHERE owner_id = ? ORDER BY account_id";

        int count = 0;

        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, ownerId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                count++;
                String type = rs.getString("account_type");

                System.out.println("──────────── Account #" + count + " ────────────");
                System.out.println("  Account ID   : " + rs.getInt("account_id"));
                System.out.println("  Owner ID     : " + rs.getInt("owner_id"));
                System.out.println("  Type         : " + type);
                System.out.println("  Status       : " + rs.getString("status"));
                System.out.println("  Balance      : $" + rs.getBigDecimal("balance"));

                if ("CHECKING".equals(type)) {
                    System.out.println("  Overdraft    : $" + rs.getBigDecimal("overdraft_limit"));
                } else if ("SAVINGS".equals(type)) {
                    BigDecimal rate = rs.getBigDecimal("interest_rate");
                    System.out.println("  Interest Rate: " + rate.multiply(new BigDecimal("100")) + "%");
                }

                System.out.println("  Created At   : " + rs.getTimestamp("created_at"));
                System.out.println("  Updated At   : " + rs.getTimestamp("updated_at"));
                System.out.println("─────────────────────────────────────────");
            }

            if (count == 0) {
                System.out.println("[DatabaseManager] No accounts found for user ID: " + ownerId);
            } else {
                System.out.println("[DatabaseManager] Total accounts found: " + count);
            }

        } catch (SQLException e) {
            System.out.println("[DatabaseManager] ERROR: Failed to fetch accounts.");
            System.out.println("Details: " + e.getMessage());
        }

        return count;
    }

    // updates balance atomically so we dont get race conditions
    // positive delta = deposit, negative = withdrawal
    public boolean updateBalance(int accountId, BigDecimal delta) {

        if (delta == null || delta.compareTo(BigDecimal.ZERO) == 0) {
            System.out.println("[DatabaseManager] ERROR: Delta amount cannot be zero or null.");
            return false;
        }

        String sql = "UPDATE accounts SET balance = balance + ? "
                + "WHERE account_id = ? AND status = 'ACTIVE' "
                + "RETURNING account_id, balance";

        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBigDecimal(1, delta);
            ps.setInt(2, accountId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                BigDecimal newBalance = rs.getBigDecimal("balance");
                String action = delta.compareTo(BigDecimal.ZERO) > 0 ? "Deposit" : "Withdrawal";
                System.out.println("[DatabaseManager] " + action + " successful.");
                System.out.println("  Account ID  : " + rs.getInt("account_id"));
                System.out.println("  Amount      : $" + delta.abs());
                System.out.println("  New Balance : $" + newBalance);
                return true;
            } else {
                System.out.println("[DatabaseManager] ERROR: Update failed. "
                        + "Account may not exist, may not be ACTIVE, or the "
                        + "transaction violates a balance constraint.");
            }

        } catch (SQLException e) {
            System.out.println("[DatabaseManager] ERROR: Balance update rejected by the database.");
            System.out.println("Details: " + e.getMessage());
        }

        return false;
    }

    // checks the password hash and returns a user object if it matches
    public User authenticateUser(String fullName, String plaintextPassword) {

        String sql = "SELECT user_id, full_name, email, password_hash, standing, access_level "
                + "FROM users WHERE full_name = ?";

        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, fullName);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");

                // verify against the pbkdf2 hash
                if (storedHash == null || !PasswordUtil.verifyPassword(plaintextPassword, storedHash)) {
                    System.out.println("[DatabaseManager] Password verification failed for: " + fullName);
                    return null;
                }

                int userId = rs.getInt("user_id");
                String name = rs.getString("full_name");
                String email = rs.getString("email");
                String standing = rs.getString("standing");
                String accessLevel = rs.getString("access_level");

                System.out.println("[DatabaseManager] User authenticated: " + name
                        + " (ID: " + userId + ")");

                return new User(userId, name, email, null, standing, accessLevel);
            } else {
                System.out.println("[DatabaseManager] No user found with name: " + fullName);
            }

        } catch (SQLException e) {
            System.out.println("[DatabaseManager] ERROR: Authentication query failed.");
            System.out.println("Details: " + e.getMessage());
        }

        return null;
    }
}