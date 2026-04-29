public class User {

    private int userId;
    private String fullName;
    private String email;
    private String password;
    private String standing;
    private String accessLevel;

    public User(int userId, String fullName, String email, String password,
            String standing, String accessLevel) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.standing = (standing != null) ? standing : "GOOD";
        this.accessLevel = (accessLevel != null) ? accessLevel : "USER";
    }

    // shortcut constructor for the signup form
    public User(int userId, String fullName, String password, String accountType) {
        this(userId, fullName, null, password, "GOOD", "USER");
    }

    // ── Getters ── 

    public int getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    // alias so the old ui code still works  
    public String getUsername() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getStanding() {
        return standing;
    }

    public String getAccessLevel() {
        return accessLevel;
    }

    // ── Setters ──

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setStanding(String standing) {
        this.standing = standing;
    }

    public void setAccessLevel(String accessLevel) {
        this.accessLevel = accessLevel;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", standing='" + standing + '\'' +
                ", accessLevel='" + accessLevel + '\'' +
                '}';
    }
}
