import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class Admin {

    // 10k reporting threshold
    private static final BigDecimal LARGE_TRANSACTION_THRESHOLD = new BigDecimal("10000.00");

    public boolean isOverThreshold(BigDecimal amount) {
        if (amount == null)
            return false;
        return amount.compareTo(LARGE_TRANSACTION_THRESHOLD) > 0;
    }

    // returns true if the transaction looks suspicious
    public boolean isSuspicious(Account account, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            return false;

        if (isOverThreshold(amount)) {
            System.out.println("Suspicious: transaction is over $10,000.");
            return true;
        }

        return false;
    }

    public String getAccountStatus(Account account) {
        if (account == null)
            return "UNKNOWN";

        BigDecimal balance = account.getBalance();

        if (balance.compareTo(BigDecimal.ZERO) < 0)
            return "OVERDRAWN";
        if (balance.compareTo(BigDecimal.ZERO) == 0)
            return "EMPTY";
        return "ACTIVE";
    }

    // adds up all account balances for a user
    public BigDecimal calculateTotalBalance(List<Account> accounts) {
        if (accounts == null)
            return BigDecimal.ZERO;

        BigDecimal total = BigDecimal.ZERO;
        for (Account account : accounts) {
            if (account != null) {
                total = total.add(account.getBalance());
            }
        }
        return total.setScale(2, RoundingMode.HALF_EVEN);
    }

}