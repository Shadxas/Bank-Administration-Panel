import java.math.BigDecimal;
import java.math.RoundingMode;

public class Account {

    protected int accountId;
    protected int ownerId;
    protected BigDecimal balance;
    protected String status;

    public Account(int accountId, int ownerId, BigDecimal balance) {
        this.accountId = accountId;
        this.ownerId = ownerId;
        this.balance = normalizeAmount(balance);
        this.status = "ACTIVE";
    }

    // rounds to 2 decimal places
    protected BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        }
        return amount.setScale(2, RoundingMode.HALF_EVEN);
    }

    public boolean deposit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("Deposit failed: amount must be positive.");
            return false;
        }
        balance = balance.add(normalizeAmount(amount));
        System.out.println("Deposit successful. New Balance: " + balance);
        return true;
    }

    // no overdraft allowed on base account
    public boolean withdraw(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("Withdrawal failed: amount must be positive.");
            return false;
        }
        if (amount.compareTo(balance) > 0) {
            System.out.println("Withdrawal failed: insufficient funds.");
            return false;
        }
        balance = balance.subtract(normalizeAmount(amount));
        System.out.println("Withdrawal successful. New Balance: " + balance);
        return true;
    }

    public int getAccountId() {
        return accountId;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public BigDecimal getBalance() {
        return this.balance;
    }

    public String getStatus() {
        return status;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = normalizeAmount(balance);
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Account{" +
                "accountId=" + accountId +
                ", ownerId=" + ownerId +
                ", balance=" + balance +
                ", status='" + status + '\'' +
                '}';
    }
}
