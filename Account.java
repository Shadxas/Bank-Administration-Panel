public class Account {
    // Account Fields
    protected int accountId;
    protected double balance;

    // Constructor
    public Account(int accountId, double balance) {
        this.accountId = accountId;
        this.balance = balance;
    }

    // withdraw and deposit methods
    public void deposit(double amount) {
        if (amount <= 0) {
            System.out.print("Enter a valid amount to deposit.");
        } else {
            balance += amount;
            System.out.print("Deposit successfull. New Balance: " + balance);
        }
    }

    public void withdraw(double amount) {
        if (amount <= 0 || amount > balance) {
            System.out.print("Enter a valid amount to withdraw.");
        } else {
            balance -= amount;
            System.out.print("Withdraw successfull. Withdrawed: " + amount + ". New Balance is: " + balance);
        }
    }

    // getter methods
    public double getBalance() {
        return this.balance;
    }

    public int getAccountId() {
        return accountId;
    }

    // Tostring method
    @Override
    public String toString() {
        return "Account{" +
                "accountId=" + accountId +
                ", balance=" + balance +
                '}';
    }
}
