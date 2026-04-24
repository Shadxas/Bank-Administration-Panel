import java.math.BigDecimal;
import java.math.RoundingMode;

public class SavingsAccount extends Account {

    private BigDecimal interestRate = new BigDecimal("0.03"); // 3% interest

    public SavingsAccount(int accountId, int ownerId, BigDecimal balance) {
        super(accountId, ownerId, balance);
    }

    @Override
    public void withdraw(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return; // cannot be negative in a savings account

        if (amount.compareTo(balance) > 0) {
            System.out.println("Cannot withdraw from savings: insufficient funds.");
        } else {
            balance = balance.subtract(amount).setScale(2, RoundingMode.HALF_EVEN);
        }
    }

    public void applyInterest() {
        balance = balance.add(balance.multiply(interestRate)).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public String toString() {
        return "SavingsAccount{id=" + accountId +
               ", ownerId=" + ownerId +
               ", balance=" + balance +
               ", interestRate=" + interestRate + "}";
    }
}