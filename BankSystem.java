import java.math.BigDecimal;

public class BankSystem {

    private int nextUserId = 1000;
    private int nextAccountId = 2000;

    // creates a new user and returns their assigned userId
    public int registerUser(String username, String password, String accountType) {
        if (username == null || username.isEmpty()) return -1;
        if (password == null || password.isEmpty() || !new PasswordValidate().isStrong(password)) return -1;
        if (accountType == null || (!accountType.equals("CHECKING") && !accountType.equals("SAVINGS"))) return -1;

        int userId = nextUserId++;
        return userId;
    }

    // creates a new account for a user and returns the accountId
    public int createAccount(int ownerId, String accountType, BigDecimal initialBalance) {
        if (accountType == null) {
            System.out.println("Account type cannot be null.");
            return -1;
        }

        int accountId = nextAccountId++;

        if (accountType.equals("CHECKING")) {
            CheckingAccount account = new CheckingAccount(accountId, ownerId, initialBalance);
            System.out.println("Checking account created: " + account);
        } else if (accountType.equals("SAVINGS")) {
            SavingsAccount account = new SavingsAccount(accountId, ownerId, initialBalance);
            System.out.println("Savings account created: " + account);
        } else {
            System.out.println("Invalid account type.");
            return -1;
        }

        return accountId;
    }

    // checks if the username and password match (database teammate will replace this)
    public boolean login(String username, String password) {
        if (username == null || password == null) return false;
        if (username.isEmpty() || password.isEmpty()) return false;

        // database teammate: query users table and verify password hash here
        System.out.println("Login successful for: " + username);
        return true;
    }

    // deposits money into an account
    public boolean deposit(Account account, BigDecimal amount) {
        if (account == null) return false;
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return false;

        account.deposit(amount);
        return true;
    }

    // withdraws money from an account
    public boolean withdraw(Account account, BigDecimal amount) {
        if (account == null) return false;
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return false;

        BigDecimal balanceBefore = account.getBalance();
        account.withdraw(amount);

        return !account.getBalance().equals(balanceBefore);
    }

    // transfers money from one account to another
    public boolean transfer(Account from, Account to, BigDecimal amount) {
        if (from == null || to == null) return false;
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return false;

        BigDecimal balanceBefore = from.getBalance();
        from.withdraw(amount);

        if (from.getBalance().equals(balanceBefore)) {
            System.out.println("Transfer failed.");
            return false;
        }

        to.deposit(amount);
        System.out.println("Transfer successful.");
        return true;
    }

    // applies interest to a savings account
    public boolean applyInterest(Account account) {
        if (!(account instanceof SavingsAccount)) {
            System.out.println("Interest can only be applied to savings accounts.");
            return false;
        }

        ((SavingsAccount) account).applyInterest();
        return true;
    }

    // returns the balance of an account
    public BigDecimal getBalance(Account account) {
        if (account == null) return null;
        return account.getBalance();
    }

}