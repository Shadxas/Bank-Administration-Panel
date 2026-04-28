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
    // throws sqlexception if the connection fails so callers catch it cleanly

    public Connection getConnection() throws SQLException {
        if (URL == null || URL.isEmpty()) {
            throw new SQLException("Database connection failed. DB_URL is not configured.");
        }
        try {
            return DriverManager.getConnection(URL);
        } catch (SQLException e) {
            System.out.println("[DatabaseManager] ERROR: Failed to connect to the database.");
            System.out.println("Check your connection string, password, and internet connection.");
            System.out.println("Details: " + e.getMessage());
            throw new SQLException("Database connection failed. Please check your internet or credentials.", e);
        }
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

    // atomic registration: inserts user + their first account in one transaction
    // if either fails the whole thing rolls back, no orphaned users
    public int registerUserWithAccount(String fullName, String email, String plaintextPassword,
            String accountType, BigDecimal initialBalance,
            BigDecimal overdraftLimit, BigDecimal interestRate) {

        String passwordHash = PasswordUtil.hashPassword(plaintextPassword);

        String userSql = "INSERT INTO users (full_name, email, password_hash, standing, access_level, admin_role) "
                + "VALUES (?, ?, ?, 'GOOD'::standing_enum, 'USER'::access_level_enum, NULL) "
                + "RETURNING user_id";

        String accountSql = "INSERT INTO accounts (owner_id, balance, account_type, status, overdraft_limit, interest_rate) "
                + "VALUES (?, ?, ?::account_type_enum, 'ACTIVE'::account_status_enum, ?, ?) "
                + "RETURNING account_id";

        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            // step 1: insert the user
            int userId;
            try (PreparedStatement userPs = conn.prepareStatement(userSql)) {
                userPs.setString(1, fullName);
                userPs.setString(2, email);
                userPs.setString(3, passwordHash);

                ResultSet rs = userPs.executeQuery();
                if (!rs.next()) {
                    conn.rollback();
                    System.out.println("[DatabaseManager] Registration failed: user insert returned no ID.");
                    return -1;
                }
                userId = rs.getInt("user_id");
                System.out.println("[DatabaseManager] Transaction: user created (ID: " + userId + ")");
            }

            // step 2: insert their first account using the new user_id
            try (PreparedStatement accPs = conn.prepareStatement(accountSql)) {
                accPs.setInt(1, userId);
                accPs.setBigDecimal(2, initialBalance != null ? initialBalance : BigDecimal.ZERO);
                accPs.setString(3, accountType);

                if (overdraftLimit != null) {
                    accPs.setBigDecimal(4, overdraftLimit);
                } else {
                    accPs.setNull(4, Types.NUMERIC);
                }

                if (interestRate != null) {
                    accPs.setBigDecimal(5, interestRate);
                } else {
                    accPs.setNull(5, Types.NUMERIC);
                }

                ResultSet rs = accPs.executeQuery();
                if (!rs.next()) {
                    conn.rollback();
                    System.out.println("[DatabaseManager] Registration failed: account insert returned no ID.");
                    return -1;
                }
                int accountId = rs.getInt("account_id");
                System.out.println("[DatabaseManager] Transaction: account created (ID: " + accountId + ")");
            }

            // both succeeded
            conn.commit();
            System.out.println("[DatabaseManager] Registration committed. User #" + userId
                    + " with " + accountType + " account.");
            return userId;

        } catch (SQLException e) {
            System.out.println("[DatabaseManager] ERROR: Registration transaction failed, rolling back.");
            System.out.println("Details: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.out.println("[DatabaseManager] ERROR: Rollback also failed.");
                    System.out.println("Details: " + rollbackEx.getMessage());
                }
            }
            return -1;

        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                    System.out.println("[DatabaseManager] WARNING: Failed to restore autoCommit.");
                }
            }
        }
    }

    // fetches all accounts for a user, returns real account objects
    public java.util.List<Account> getAccountsForUser(int ownerId) {
        java.util.List<Account> accounts = new java.util.ArrayList<Account>();

        String sql = "SELECT account_id, owner_id, balance, account_type, status, "
                + "overdraft_limit, interest_rate "
                + "FROM accounts WHERE owner_id = ? AND status = 'ACTIVE' ORDER BY account_id";

        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, ownerId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int accountId = rs.getInt("account_id");
                int owner = rs.getInt("owner_id");
                BigDecimal balance = rs.getBigDecimal("balance");
                String type = rs.getString("account_type");

                if ("CHECKING".equals(type)) {
                    BigDecimal overdraft = rs.getBigDecimal("overdraft_limit");
                    accounts.add(new CheckingAccount(accountId, owner, balance, overdraft));
                } else if ("SAVINGS".equals(type)) {
                    BigDecimal rate = rs.getBigDecimal("interest_rate");
                    accounts.add(new SavingsAccount(accountId, owner, balance, rate));
                }
            }

            System.out.println("[DatabaseManager] Loaded " + accounts.size()
                    + " accounts for user ID: " + ownerId);

        } catch (SQLException e) {
            System.out.println("[DatabaseManager] ERROR: Failed to fetch accounts.");
            System.out.println("Details: " + e.getMessage());
        }

        return accounts;
    }

    // fetches all users from the db for admin panel
    public java.util.List<User> getAllUsers() {
        java.util.List<User> users = new java.util.ArrayList<User>();

        String sql = "SELECT user_id, full_name, email, standing, access_level "
                + "FROM users ORDER BY user_id";

        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                users.add(new User(
                        rs.getInt("user_id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        null,
                        rs.getString("standing"),
                        rs.getString("access_level")));
            }

        } catch (SQLException e) {
            System.out.println("[DatabaseManager] ERROR: Failed to fetch all users.");
            System.out.println("Details: " + e.getMessage());
        }

        return users;
    }

    // returns total money across all active accounts
    public BigDecimal getTotalSystemBalance() {
        String sql = "SELECT COALESCE(SUM(balance), 0) AS total FROM accounts WHERE status = 'ACTIVE'";

        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getBigDecimal("total");
            }

        } catch (SQLException e) {
            System.out.println("[DatabaseManager] ERROR: Failed to get total balance.");
            System.out.println("Details: " + e.getMessage());
        }

        return BigDecimal.ZERO;
    }

    // returns [totalAccounts, checkingCount, savingsCount]
    public int[] getAccountCountsByType() {
        int[] counts = { 0, 0, 0 };

        String sql = "SELECT account_type, COUNT(*) AS cnt "
                + "FROM accounts WHERE status = 'ACTIVE' GROUP BY account_type";

        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String type = rs.getString("account_type");
                int cnt = rs.getInt("cnt");
                counts[0] += cnt;
                if ("CHECKING".equals(type))
                    counts[1] = cnt;
                else if ("SAVINGS".equals(type))
                    counts[2] = cnt;
            }

        } catch (SQLException e) {
            System.out.println("[DatabaseManager] ERROR: Failed to get account counts.");
            System.out.println("Details: " + e.getMessage());
        }

        return counts;
    }

    // deposits money into an account, returns the new balance or null if it failed
    public BigDecimal deposit(int accountId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("[DatabaseManager] ERROR: Deposit amount must be positive.");
            return null;
        }

        String sql = "UPDATE accounts SET balance = balance + ? "
                + "WHERE account_id = ? AND status = 'ACTIVE' "
                + "RETURNING balance";

        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBigDecimal(1, amount);
            ps.setInt(2, accountId);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                BigDecimal newBalance = rs.getBigDecimal("balance");
                System.out.println("[DatabaseManager] Deposit of $" + amount
                        + " successful. New balance: $" + newBalance);
                return newBalance;
            } else {
                System.out.println("[DatabaseManager] Deposit failed. Account may not exist or is not ACTIVE.");
            }

        } catch (SQLException e) {
            System.out.println("[DatabaseManager] ERROR: Deposit rejected by the database.");
            System.out.println("Details: " + e.getMessage());
        }

        return null;
    }

    // withdraws money from an account, enforces overdraft/savings rules atomically
    // returns the new balance or null if the withdrawal was rejected
    public BigDecimal withdraw(int accountId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("[DatabaseManager] ERROR: Withdrawal amount must be positive.");
            return null;
        }

        // atomic conditional update that respects account type rules:
        // savings: balance after withdrawal must be >= 0
        // checking: balance after withdrawal must be >= -overdraft_limit
        String sql = "UPDATE accounts SET balance = balance - ? "
                + "WHERE account_id = ? AND status = 'ACTIVE' "
                + "AND ("
                + "  (account_type = 'SAVINGS'  AND balance - ? >= 0) OR "
                + "  (account_type = 'CHECKING' AND balance - ? >= -overdraft_limit)"
                + ") "
                + "RETURNING balance";

        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBigDecimal(1, amount);
            ps.setInt(2, accountId);
            ps.setBigDecimal(3, amount);
            ps.setBigDecimal(4, amount);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                BigDecimal newBalance = rs.getBigDecimal("balance");
                System.out.println("[DatabaseManager] Withdrawal of $" + amount
                        + " successful. New balance: $" + newBalance);
                return newBalance;
            } else {
                System.out.println("[DatabaseManager] Withdrawal rejected. "
                        + "Insufficient funds, overdraft limit reached, or account not ACTIVE.");
            }

        } catch (SQLException e) {
            System.out.println("[DatabaseManager] ERROR: Withdrawal rejected by the database.");
            System.out.println("Details: " + e.getMessage());
        }

        return null;
    }

    // atomic transfer between two accounts using a single transaction
    // if anything fails the whole thing gets rolled back, no ghost money
    public boolean transferFunds(int fromAccountId, int toAccountId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("[DatabaseManager] ERROR: Transfer amount must be positive.");
            return false;
        }

        // withdraw sql respects overdraft/savings rules
        String withdrawSql = "UPDATE accounts SET balance = balance - ? "
                + "WHERE account_id = ? AND status = 'ACTIVE' "
                + "AND ("
                + "  (account_type = 'SAVINGS'  AND balance - ? >= 0) OR "
                + "  (account_type = 'CHECKING' AND balance - ? >= -overdraft_limit)"
                + ") "
                + "RETURNING balance";

        String depositSql = "UPDATE accounts SET balance = balance + ? "
                + "WHERE account_id = ? AND status = 'ACTIVE' "
                + "RETURNING balance";

        Connection conn = null;
        try {
            conn = getConnection();

            // start the transaction
            conn.setAutoCommit(false);

            // step 1: withdraw from the source account
            try (PreparedStatement withdrawPs = conn.prepareStatement(withdrawSql)) {
                withdrawPs.setBigDecimal(1, amount);
                withdrawPs.setInt(2, fromAccountId);
                withdrawPs.setBigDecimal(3, amount);
                withdrawPs.setBigDecimal(4, amount);

                ResultSet rs = withdrawPs.executeQuery();
                if (!rs.next()) {
                    // withdrawal rejected, roll back and bail
                    conn.rollback();
                    System.out.println("[DatabaseManager] Transfer failed: withdrawal from account #"
                            + fromAccountId + " was rejected (insufficient funds or not ACTIVE).");
                    return false;
                }
                System.out.println("[DatabaseManager] Transfer withdraw OK. Source balance: $"
                        + rs.getBigDecimal("balance"));
            }

            // step 2: deposit into the destination account
            try (PreparedStatement depositPs = conn.prepareStatement(depositSql)) {
                depositPs.setBigDecimal(1, amount);
                depositPs.setInt(2, toAccountId);

                ResultSet rs = depositPs.executeQuery();
                if (!rs.next()) {
                    // deposit failed, roll back the withdrawal too
                    conn.rollback();
                    System.out.println("[DatabaseManager] Transfer failed: deposit to account #"
                            + toAccountId + " failed (account may not exist or not ACTIVE).");
                    return false;
                }
                System.out.println("[DatabaseManager] Transfer deposit OK. Dest balance: $"
                        + rs.getBigDecimal("balance"));
            }

            // both succeeded, commit the transaction
            conn.commit();
            System.out.println("[DatabaseManager] Transfer of $" + amount + " from #"
                    + fromAccountId + " to #" + toAccountId + " committed successfully.");
            return true;

        } catch (SQLException e) {
            // something blew up, roll everything back
            System.out.println("[DatabaseManager] ERROR: Transfer failed, rolling back.");
            System.out.println("Details: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.out.println("[DatabaseManager] ERROR: Rollback also failed.");
                    System.out.println("Details: " + rollbackEx.getMessage());
                }
            }
            return false;

        } finally {
            // always restore autocommit and close the connection
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                    System.out.println("[DatabaseManager] WARNING: Failed to restore autoCommit.");
                }
            }
        }
    }

    // checks the password hash and returns a user object if it matches
    // matches on email since its unique, full_name is not
    public User authenticateUser(String email, String plaintextPassword) {

        String sql = "SELECT user_id, full_name, email, password_hash, standing, access_level "
                + "FROM users WHERE email = ?";

        try (Connection conn = getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");

                // verify against the pbkdf2 hash
                if (storedHash == null || !PasswordUtil.verifyPassword(plaintextPassword, storedHash)) {
                    System.out.println("[DatabaseManager] Password verification failed for: " + email);
                    return null;
                }

                int userId = rs.getInt("user_id");
                String name = rs.getString("full_name");
                String userEmail = rs.getString("email");
                String standing = rs.getString("standing");
                String accessLevel = rs.getString("access_level");

                System.out.println("[DatabaseManager] User authenticated: " + name
                        + " (ID: " + userId + ", access: " + accessLevel + ")");

                return new User(userId, name, userEmail, null, standing, accessLevel);
            } else {
                System.out.println("[DatabaseManager] No user found with email: " + email);
            }

        } catch (SQLException e) {
            System.out.println("[DatabaseManager] ERROR: Authentication query failed.");
            System.out.println("Details: " + e.getMessage());
        }

        return null;
    }
}