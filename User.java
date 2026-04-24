public class User {
    // user variables
    private int userId;
    private String username;
    private String password;
    private String accountType;

    // constructors
    public User(int userId, String username, String password, String accountType) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.accountType = accountType;
    }

    // getters
    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getAccountType() {
        return accountType;
    }

}
