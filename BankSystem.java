import java.math.BigDecimal;

public class BankSystem {

    private DatabaseManager dbManager;

    public BankSystem(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    // atomic registration: user + their first account in one transaction
    public int registerUserWithAccount(String fullName, String email, String password,
            String accountType, BigDecimal initialBalance) {
        if (fullName == null || fullName.isEmpty())
            return -1;
        if (password == null || password.isEmpty())
            return -1;
        if (!new PasswordValidate().isStrong(password))
            return -1;
        if (accountType == null || (!accountType.equals("CHECKING") && !accountType.equals("SAVINGS")))
            return -1;

        BigDecimal overdraftLimit = null;
        BigDecimal interestRate = null;

        if ("CHECKING".equals(accountType)) {
            overdraftLimit = new BigDecimal("500.00");
        } else {
            interestRate = new BigDecimal("0.0300");
        }

        return dbManager.registerUserWithAccount(fullName, email, password,
                accountType, initialBalance, overdraftLimit, interestRate);
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

    // tries to log in, returns a user object or null
    public User authenticateUser(String email, String password) {
        if (email == null || email.isEmpty())
            return null;
        if (password == null || password.isEmpty())
            return null;

        return dbManager.authenticateUser(email, password);
    }

    // db-backed deposit, returns the new balance or null on failure
    public BigDecimal depositToAccount(int accountId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            return null;

        return dbManager.deposit(accountId, amount);
    }

    // db-backed withdraw, returns the new balance or null on failure
    public BigDecimal withdrawFromAccount(int accountId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            return null;

        return dbManager.withdraw(accountId, amount);
    }

    // db-backed atomic transfer, returns true if the whole thing committed
    public boolean transferBetweenAccounts(int fromAccountId, int toAccountId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            return false;
        if (fromAccountId == toAccountId)
            return false;

        return dbManager.transferFunds(fromAccountId, toAccountId, amount);
    }

    public BigDecimal getBalance(Account account) {
        if (account == null)
            return null;
        return account.getBalance();
    }

    // fetches real accounts from the database
    public java.util.List<Account> getAccountsForUser(int userId) {
        return dbManager.getAccountsForUser(userId);
    }

    // fetches all users from the database
    public java.util.List<User> getAllUsers() {
        return dbManager.getAllUsers();
    }

    // total money across all active accounts
    public BigDecimal getTotalSystemBalance() {
        return dbManager.getTotalSystemBalance();
    }

    // returns [total, checking, savings] counts
    public int[] getAccountCountsByType() {
        return dbManager.getAccountCountsByType();
    }
}