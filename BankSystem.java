import java.math.BigDecimal;

public class BankSystem {

    private DatabaseManager dbManager;

    // fallback ids for when theres no database
    private int nextUserId = 1000;
    private int nextAccountId = 2000;

    public BankSystem() {
        this.dbManager = null;
    }

    public BankSystem(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    // register user in the db, returns the new user id or -1
    public int registerUser(String fullName, String email, String password) {
        if (fullName == null || fullName.isEmpty())
            return -1;
        if (password == null || password.isEmpty())
            return -1;
        if (!new PasswordValidate().isStrong(password))
            return -1;

        // Database path
        if (dbManager != null) {
            return dbManager.registerUser(fullName, email, password, "GOOD", "USER", null);
        }

        // Fallback: in-memory (no DB)
        int userId = nextUserId++;
        System.out.println("[BankSystem] (offline) User registered with ID: " + userId);
        return userId;
    }

    // create a bank account, returns the new account id or -1
    public int createAccount(int ownerId, String accountType, BigDecimal initialBalance) {
        if (accountType == null) {
            System.out.println("Account type cannot be null.");
            return -1;
        }
        if (!accountType.equals("CHECKING") && !accountType.equals("SAVINGS")) {
            System.out.println("Invalid account type.");
            return -1;
        }

        if (dbManager != null) {
            BigDecimal overdraftLimit = null;
            BigDecimal interestRate = null;

            if ("CHECKING".equals(accountType)) {
                overdraftLimit = new BigDecimal("500.00");
            } else {
                interestRate = new BigDecimal("0.0300");
            }

            return dbManager.createAccount(ownerId, initialBalance, accountType,
                    overdraftLimit, interestRate);
        }

        int accountId = nextAccountId++;

        if (accountType.equals("CHECKING")) {
            CheckingAccount account = new CheckingAccount(accountId, ownerId, initialBalance);
            System.out.println("Checking account created: " + account);
        } else {
            SavingsAccount account = new SavingsAccount(accountId, ownerId, initialBalance);
            System.out.println("Savings account created: " + account);
        }

        return accountId;
    }

    // tries to log in, returns a user object or null
    public User authenticateUser(String fullName, String password) {
        if (fullName == null || fullName.isEmpty())
            return null;
        if (password == null || password.isEmpty())
            return null;

        if (dbManager != null) {
            return dbManager.authenticateUser(fullName, password);
        }

        System.out.println("[BankSystem] (offline) No database — cannot authenticate.");
        return null;
    }

    public boolean deposit(Account account, BigDecimal amount) {
        if (account == null)
            return false;
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            return false;

        return account.deposit(amount);
    }

    public boolean withdraw(Account account, BigDecimal amount) {
        if (account == null)
            return false;
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            return false;

        return account.withdraw(amount);
    }

    // db-backed deposit, returns the new balance or null on failure
    public BigDecimal depositToAccount(int accountId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            return null;

        if (dbManager != null) {
            return dbManager.deposit(accountId, amount);
        }

        System.out.println("[BankSystem] (offline) No database for deposit.");
        return null;
    }

    // db-backed withdraw, returns the new balance or null on failure
    public BigDecimal withdrawFromAccount(int accountId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            return null;

        if (dbManager != null) {
            return dbManager.withdraw(accountId, amount);
        }

        System.out.println("[BankSystem] (offline) No database for withdrawal.");
        return null;
    }

    public boolean transfer(Account from, Account to, BigDecimal amount) {
        if (from == null || to == null)
            return false;
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            return false;

        if (!from.withdraw(amount)) {
            System.out.println("Transfer failed: withdrawal from source rejected.");
            return false;
        }

        to.deposit(amount);
        System.out.println("Transfer successful.");
        return true;
    }

    // db-backed atomic transfer, returns true if the whole thing committed
    public boolean transferBetweenAccounts(int fromAccountId, int toAccountId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            return false;
        if (fromAccountId == toAccountId)
            return false;

        if (dbManager != null) {
            return dbManager.transferFunds(fromAccountId, toAccountId, amount);
        }

        System.out.println("[BankSystem] (offline) No database for transfer.");
        return false;
    }

    public boolean applyInterest(Account account) {
        if (!(account instanceof SavingsAccount)) {
            System.out.println("Interest can only be applied to savings accounts.");
            return false;
        }

        ((SavingsAccount) account).applyInterest();
        return true;
    }

    public BigDecimal getBalance(Account account) {
        if (account == null)
            return null;
        return account.getBalance();
    }
}