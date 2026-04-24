import java.math.BigDecimal;
import java.math.RoundingMode;

public class Account {
    // Account Fields
    protected int accountId;
    protected int ownerId;
    protected BigDecimal balance;

    // Constructor
    public Account(int accountId, int ownerId, BigDecimal balance) {
        this.accountId = accountId;
        this.ownerId = ownerId;
        this.balance = normalizeAmount(balance);
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        }
        return amount.setScale(2, RoundingMode.HALF_EVEN);
    }

    // withdraw and deposit methods
    public void deposit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.print("Enter a valid amount to deposit.");
        } else {
            balance = balance.add(normalizeAmount(amount));
            System.out.print("Deposit successful. New Balance: " + balance);
        }
    }

    public void withdraw(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0 || amount.compareTo(balance) > 0) {
            System.out.print("Enter a valid amount to withdraw.");
        } else {
            balance = balance.subtract(normalizeAmount(amount));
            System.out.print("Withdraw successful. Withdrawed: " + amount + ". New Balance is: " + balance);
        }
    }

    // getter and setter methods
    public BigDecimal getBalance() {
        return this.balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = normalizeAmount(balance);
    }

    public int getAccountId() {
        return accountId;
    }

    public int getOwnerId() {
        return ownerId;
    }

    // Tostring method
    @Override
    public String toString() {
        return "Account{" +
                "accountId=" + accountId +
                ", ownerId=" + ownerId +
                ", balance=" + balance +
                '}';
    }
}
