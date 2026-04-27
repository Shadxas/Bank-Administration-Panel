import java.math.BigDecimal;

public class Main {
    public static void main(String[] args) {
        // Testing DatabaseManager
        // 1. Welcome Message
        System.out.println("==========================================");
        System.out.println("   Bank Administration Panel Booting Up   ");
        System.out.println("==========================================\n");

        System.out.println("[System] Initializing application...");

        // 2. Initialize the Database Connection
        System.out.println("[System] Connecting to Supabase database...\n");
        DatabaseManager dbManager = new DatabaseManager();

        // 3. Test the Database Methods
        System.out.println("\n--- RUNNING DATABASE TESTS ---");

        // Step A: Create a new user
        int userId = dbManager.registerUser("Test User", "test@example.com", "GOOD", "USER", null);

        // Check if user creation was successful before continuing
        if (userId != -1) {
            // Step B: Create a checking account for that user
            int acctId = dbManager.createAccount(userId, new BigDecimal("1000.00"), "CHECKING",
                    new BigDecimal("500.00"), null);

            if (acctId != -1) {
                // Step C: Deposit $250.00
                dbManager.updateBalance(acctId, new BigDecimal("250.00"));

                // Step D: Print out the final account details to prove it worked
                System.out.println("\n--- FINAL ACCOUNT STATE ---");
                dbManager.getAccountsByUser(userId);
            }
        } else {
            System.out.println("[System] Skipping account tests because user registration failed.");
        }

        System.out.println("\n[System] System is ready.");
        System.out.println("[System] Awaiting BankSystem and UI implementation...");
    }
}