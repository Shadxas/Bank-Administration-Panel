import java.math.BigDecimal;
import java.math.RoundingMode;

public class SavingsAccount extends Account {

    private BigDecimal interestRate;

    public SavingsAccount(int accountId, int ownerId, BigDecimal balance) {
        super(accountId, ownerId, balance);
        this.interestRate = new BigDecimal("0.0300");
    }

    // lets you set a custom rate from the db 
    public SavingsAccount(int accountId, int ownerId, BigDecimal balance, BigDecimal interestRate) {
        super(accountId, ownerId, balance);
        this.interestRate = (interestRate != null)
                ? interestRate.setScale(4, RoundingMode.HALF_EVEN)
                : new BigDecimal("0.0300");
    }

    // savings cant go below zero
    @Override
    public boolean withdraw(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("Withdrawal failed: amount must be positive.");
            return false;
        }
        if (amount.compareTo(balance) > 0) {
            System.out.println("Withdrawal failed: savings accounts cannot go below $0.");
            return false;
        }
        balance = balance.subtract(amount).setScale(2, RoundingMode.HALF_EVEN);
        System.out.println("Withdrawal successful. New balance: " + balance);
        return true;
    }

    public void applyInterest() {
        balance = balance.add(balance.multiply(interestRate)).setScale(2, RoundingMode.HALF_EVEN);
        System.out.println("Interest applied. New balance: " + balance);
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = (interestRate != null)
                ? interestRate.setScale(4, RoundingMode.HALF_EVEN)
                : BigDecimal.ZERO;
    }

    @Override
    public String toString() {
        return "SavingsAccount{id=" + accountId +
                ", ownerId=" + ownerId +
                ", balance=" + balance +
                ", interestRate=" + interestRate +
                ", status='" + status + '\'' +
                "}";
    }
}