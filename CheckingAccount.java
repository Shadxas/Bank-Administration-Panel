public class CheckingAccount extends Account {
    private double overdraftLimit = 500;

    public CheckingAccount(int accountId, double balance) {
        super(accountId, balance);
    }

    @Override
    public void withdraw(double amount) {
        if (amount <= 0) {
            System.out.print("Invalid withdrawal amount.");
            return;
        }

        // allows overdraft
        if (balance - amount < -overdraftLimit) {
            System.out.print("Overdraft limit exceeded.");
        } else {
            balance -= amount;
            System.out.println("Withdrawal successful.");
            System.out.println("New balance: " + balance); // balance can be negative but only within the limit
        }
    }

    @Override
    public String toString() {
        return "CheckingAccount{id=" + accountId +
                ", balance=" + balance +
                ", overdraftLimit=" + overdraftLimit + "}";
    }
}
