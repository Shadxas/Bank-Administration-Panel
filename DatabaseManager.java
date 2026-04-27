import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class DatabaseManager {

    private static final String URL = "jdbc:postgresql://aws-1-us-east-2.pooler.supabase.com:5432/postgres?user=postgres.ocfzxkxeggkjtlokyazx&password=Wp2mw6E6GtZN5svP";

    public DatabaseManager() {
        // Load the PostgreSQL JDBC Driver when the manager is instantiated
        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("[DatabaseManager] PostgreSQL JDBC Driver loaded successfully.");
        } catch (ClassNotFoundException e) {
            System.out.println("[DatabaseManager] ERROR: PostgreSQL JDBC Driver not found.");
            System.out.println("Make sure the .jar file is in your 'lib' folder and linked to the project.");
            e.printStackTrace();
        }
    }

    /**
     * Establishes and returns a connection to the Supabase database.
     * 
     * @return Connection object, or null if connection fails
     */
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

    // =========================================================================
    // 1. REGISTER A NEW USER (User or Admin)
    // =========================================================================
    /**
     * Registers a new user in the database (either a regular User or an Admin).
     *
     * For a regular user: pass accessLevel = "USER" and adminRole = null.
     * For an admin: pass accessLevel = "ADMIN" and adminRole = e.g. "SUPER_ADMIN".
     *
     * Valid accessLevel values: "USER", "ADMIN"
     * Valid standing values: "GOOD", "FAIR", "POOR", "SUSPENDED"
     * Valid adminRole values: "SUPER_ADMIN", "BRANCH_MANAGER", "SUPPORT" (or null
     * for users)
     *
     * @param fullName    the user's full name
     * @param email       the user's email (must be unique)
     * @param standing    the user's standing (defaults to GOOD in DB if null passed
     *                    here)
     * @param accessLevel "USER" or "ADMIN"
     * @param adminRole   the admin role (required if ADMIN, must be null if USER)
     * @return the generated user_id, or -1 if the operation fails
     */
    public int registerUser(String fullName, String email, String standing,
            String accessLevel, String adminRole) {

        String sql = "INSERT INTO users (full_name, email, standing, access_level, admin_role) "
                + "VALUES (?, ?, ?::standing_enum, ?::access_level_enum, ?::admin_role_enum) "
                + "RETURNING user_id";

        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setString(3, standing != null ? standing : "GOOD");
            ps.setString(4, accessLevel != null ? accessLevel : "USER");

            // adminRole can be null for regular users
            if (adminRole != null) {
                ps.setString(5, adminRole);
            } else {
                ps.setNull(5, Types.OTHER);
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

    // =========================================================================
    // 2. CREATE A NEW BANK ACCOUNT (Checking or Savings)
    // =========================================================================
    /**
     * Creates a new bank account linked to a user.
     *
     * For a CHECKING account: pass overdraftLimit (e.g. 500.00) and interestRate =
     * null.
     * For a SAVINGS account: pass interestRate (e.g. 0.03) and overdraftLimit =
     * null.
     *
     * The database CHECK constraints enforce that the correct subclass field is
     * set.
     *
     * @param ownerId        the user_id of the account owner
     * @param initialBalance the starting balance (use BigDecimal for precision)
     * @param accountType    "CHECKING" or "SAVINGS"
     * @param overdraftLimit the overdraft limit (CHECKING only, null for SAVINGS)
     * @param interestRate   the interest rate (SAVINGS only, null for CHECKING)
     * @return the generated account_id, or -1 if the operation fails
     */
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

            // overdraft_limit — required for CHECKING, null for SAVINGS
            if (overdraftLimit != null) {
                ps.setBigDecimal(4, overdraftLimit);
            } else {
                ps.setNull(4, Types.NUMERIC);
            }

            // interest_rate — required for SAVINGS, null for CHECKING
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

    // =========================================================================
    // 3. FETCH A USER'S DETAILS BY ID
    // =========================================================================
    /**
     * Fetches and prints a user's full details from the database by their user_id.
     * Returns true if the user was found, false otherwise.
     *
     * This returns all STI columns; the caller can inspect access_level to know
     * if the row represents a User or an Admin.
     *
     * @param userId the user_id to look up
     * @return true if a user with that ID exists, false otherwise
     */
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

    // =========================================================================
    // 4. FETCH ALL ACCOUNTS BELONGING TO A SPECIFIC USER
    // =========================================================================
    /**
     * Fetches and prints all bank accounts owned by the specified user.
     * Returns the number of accounts found.
     *
     * The output adapts to the account_type, printing overdraft_limit for
     * CHECKING accounts and interest_rate for SAVINGS accounts.
     *
     * @param ownerId the user_id whose accounts to retrieve
     * @return the number of accounts found (0 if none)
     */
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

    // =========================================================================
    // 5. UPDATE AN ACCOUNT BALANCE (Deposit / Withdraw)
    // =========================================================================
    /**
     * Updates an account's balance by applying a delta amount.
     *
     * For a DEPOSIT: pass a positive BigDecimal (e.g. new BigDecimal("200.00"))
     * For a WITHDRAWAL: pass a negative BigDecimal (e.g. new BigDecimal("-150.00"))
     *
     * The update is performed atomically with a single SQL statement:
     * SET balance = balance + delta
     * This avoids read-then-write race conditions.
     *
     * The database CHECK constraints will reject the update if:
     * - A SAVINGS balance would go below 0.
     * - A CHECKING balance would exceed its overdraft_limit below 0.
     *
     * @param accountId the account to update
     * @param delta     the amount to add (positive = deposit, negative =
     *                  withdrawal)
     * @return true if the update succeeded, false otherwise
     */
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
            // The CHECK constraints will throw here if balance goes out of bounds
            System.out.println("[DatabaseManager] ERROR: Balance update rejected by the database.");
            System.out.println("Details: " + e.getMessage());
        }

        return false;
    }
}