import java.math.BigDecimal;
import java.math.RoundingMode;

public class CheckingAccount extends Account {

    // Default $500 overdraft — will be overridden by the DB value once wiring is
    // complete.
    private BigDecimal overdraftLimit;

    // ── Constructors ──

    // Default overdraft limit ($500)
    public CheckingAccount(int accountId, int ownerId, BigDecimal balance) {
        super(accountId, ownerId, balance);
        this.overdraftLimit = new BigDecimal("500.00");
    }

    // lets you set a custom overdraft from the db
    public CheckingAccount(int accountId, int ownerId, BigDecimal balance, BigDecimal overdraftLimit) {
        super(accountId, ownerId, balance);
        this.overdraftLimit = (overdraftLimit != null)
                ? overdraftLimit.setScale(2, RoundingMode.HALF_EVEN)
                : new BigDecimal("500.00");
    }

    // checking can go negative up to the overdraft limit
    @Override
    public boolean withdraw(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("Withdrawal failed: amount must be positive.");
            return false;
        }

        BigDecimal resultingBalance = balance.subtract(amount);
        if (resultingBalance.compareTo(overdraftLimit.negate()) < 0) {
            System.out.println("Withdrawal failed: overdraft limit of $" + overdraftLimit + " exceeded.");
            return false;
        }

        balance = resultingBalance.setScale(2, RoundingMode.HALF_EVEN);
        System.out.println("Withdrawal successful. New balance: " + balance);
        return true;
    }

    public BigDecimal getOverdraftLimit() {
        return overdraftLimit;
    }

    public void setOverdraftLimit(BigDecimal overdraftLimit) {
        this.overdraftLimit = (overdraftLimit != null)
                ? overdraftLimit.setScale(2, RoundingMode.HALF_EVEN)
                : BigDecimal.ZERO;
    }

    @Override
    public String toString() {
        return "CheckingAccount{id=" + accountId +
                ", ownerId=" + ownerId +
                ", balance=" + balance +
                ", overdraftLimit=" + overdraftLimit +
                ", status='" + status + '\'' +
                "}";
    }
}
