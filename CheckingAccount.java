import java.math.BigDecimal;
import java.math.RoundingMode;

public class CheckingAccount extends Account {
    private BigDecimal overdraftLimit = new BigDecimal("500.00");

    public CheckingAccount(int accountId, int ownerId, BigDecimal balance) {
        super(accountId, ownerId, balance);
    }

    @Override
    public void withdraw(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.print("Invalid withdrawal amount.");
            return;
        }

        // allows overdraft
        if (balance.subtract(amount).compareTo(overdraftLimit.negate()) < 0) {
            System.out.print("Overdraft limit exceeded.");
        } else {
            balance = balance.subtract(amount).setScale(2, RoundingMode.HALF_EVEN);
            System.out.println("Withdrawal successful.");
            System.out.println("New balance: " + balance);
        }
    }

    @Override
    public String toString() {
        return "CheckingAccount{id=" + accountId +
                ", ownerId=" + ownerId +
                ", balance=" + balance +
                ", overdraftLimit=" + overdraftLimit + "}";
    }
}
