public class SavingsAccount extends Account {

    private double interestRate = 0.03; // 3% interest

    public SavingsAccount(int accountId, double balance) {
        super(accountId, balance);
    }

    @Override
    public void withdraw(double amount) {
        if (amount <= 0) return; // cannot be negative in a savings account

        if (amount > balance) {
            System.out.println("Cannot withdraw from savings: insufficient funds.");
        } else {
            balance -= amount;
        }
    }

    public void applyInterest() {
        balance += balance * interestRate;
    }

    @Override
    public String toString() {
        return "SavingsAccount{id=" + accountId +
               ", balance=" + balance +
               ", interestRate=" + interestRate + "}";
    }
}