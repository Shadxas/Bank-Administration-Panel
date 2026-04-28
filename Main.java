import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Main {

    // hardcoded admin creds
    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "Admin123!";

    public static void main(String[] args) {
        // set native look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }

        final DatabaseManager dbManager = new DatabaseManager();
        final BankSystem bankSystem = new BankSystem(dbManager);

        // launch the login window
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new LoginFrame(bankSystem).setVisible(true);
            }
        });
    }
}

// shared colors and helpers used by all the screens
class Theme {
    public static final Color PRIMARY = new Color(20, 45, 90); // deep navy blue
    public static final Color PRIMARY_DARK = new Color(12, 28, 60); // darker navy
    public static final Color ACCENT = new Color(212, 175, 55); // gold
    public static final Color ACCENT_HOVER = new Color(230, 195, 80); // lighter gold
    public static final Color BG = new Color(245, 247, 250); // soft off-white
    public static final Color CARD = Color.WHITE;
    public static final Color TEXT = new Color(40, 40, 40);
    public static final Color SUBTEXT = new Color(110, 115, 130);
    public static final Color SUCCESS = new Color(40, 130, 80);
    public static final Color DANGER = new Color(190, 50, 50);

    public static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 28);
    public static final Font FONT_HEADING = new Font("SansSerif", Font.BOLD, 18);
    public static final Font FONT_LABEL = new Font("SansSerif", Font.PLAIN, 14);
    public static final Font FONT_FIELD = new Font("SansSerif", Font.PLAIN, 14);
    public static final Font FONT_MONO = new Font("Monospaced", Font.PLAIN, 14);

    // styled primary button (navy with gold hover)
    public static JButton primaryButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.BOLD, 14));
        b.setForeground(Color.WHITE);
        b.setBackground(PRIMARY);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setContentAreaFilled(true);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(10, 22, 10, 22));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        final Color normal = PRIMARY;
        final Color hover = ACCENT;
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                b.setBackground(hover);
                b.setForeground(PRIMARY_DARK);
            }

            public void mouseExited(MouseEvent e) {
                b.setBackground(normal);
                b.setForeground(Color.WHITE);
            }
        });
        return b;
    }

    // styled secondary button (gray outline)
    public static JButton secondaryButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.PLAIN, 14));
        b.setForeground(PRIMARY);
        b.setBackground(Color.WHITE);
        b.setOpaque(true);
        b.setBorderPainted(true);
        b.setContentAreaFilled(true);
        b.setFocusPainted(false);
        b.setBorder(new CompoundBorder(
                new LineBorder(new Color(200, 205, 215), 1),
                new EmptyBorder(8, 18, 8, 18)));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    // styled action button used on the dashboard sidebar
    public static JButton sidebarButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.PLAIN, 14));
        b.setForeground(Color.WHITE);
        b.setBackground(PRIMARY);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setContentAreaFilled(true);
        b.setFocusPainted(false);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setBorder(new EmptyBorder(12, 18, 12, 18));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                b.setBackground(ACCENT);
                b.setForeground(PRIMARY_DARK);
            }

            public void mouseExited(MouseEvent e) {
                b.setBackground(PRIMARY);
                b.setForeground(Color.WHITE);
            }
        });
        return b;
    }

    // styled text field
    public static void styleField(JTextField field) {
        field.setFont(FONT_FIELD);
        field.setBorder(new CompoundBorder(
                new LineBorder(new Color(210, 215, 225), 1, true),
                new EmptyBorder(8, 10, 8, 10)));
    }

    // a white "card" panel with a soft shadow-like border
    public static JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(CARD);
        p.setBorder(new CompoundBorder(
                new LineBorder(new Color(225, 230, 240), 1, true),
                new EmptyBorder(20, 24, 20, 24)));
        return p;
    }
}

// keeps users and accounts in temp. memory until the database is ready
class GuiState {

    private Map<String, User> usersByName = new HashMap<String, User>();
    private Map<Integer, List<Account>> accountsByUserId = new HashMap<Integer, List<Account>>();
    private Set<String> onlineUsers = new HashSet<String>();

    public boolean userExists(String username) {
        if (username == null)
            return false;
        return usersByName.containsKey(username);
    }

    public void addUser(User user) {
        if (user == null)
            return;
        usersByName.put(user.getUsername(), user);
        accountsByUserId.put(user.getUserId(), new ArrayList<Account>());
    }

    public User getUser(String username) {
        if (username == null)
            return null;
        return usersByName.get(username);
    }

    public List<User> getAllUsers() {
        return new ArrayList<User>(usersByName.values());
    }

    public void addAccount(int userId, Account account) {
        if (account == null)
            return;
        List<Account> list = accountsByUserId.get(userId);
        if (list == null) {
            list = new ArrayList<Account>();
            accountsByUserId.put(userId, list);
        }
        list.add(account);
    }

    public List<Account> getAccountsForUser(int userId) {
        List<Account> list = accountsByUserId.get(userId);
        if (list == null)
            return new ArrayList<Account>();
        return list;
    }

    public List<Account> getAllAccounts() {
        List<Account> all = new ArrayList<Account>();
        for (List<Account> l : accountsByUserId.values()) {
            all.addAll(l);
        }
        return all;
    }

    public void markOnline(String username) {
        if (username != null)
            onlineUsers.add(username);
    }

    public void markOffline(String username) {
        if (username != null)
            onlineUsers.remove(username);
    }

    public int getOnlineUserCount() {
        return onlineUsers.size();
    }

    public int countCheckingAccounts() {
        int n = 0;
        for (Account a : getAllAccounts())
            if (a instanceof CheckingAccount)
                n++;
        return n;
    }

    public int countSavingsAccounts() {
        int n = 0;
        for (Account a : getAllAccounts())
            if (a instanceof SavingsAccount)
                n++;
        return n;
    }

    public BigDecimal getTotalMoneyInBank() {
        return new Admin().calculateTotalBalance(getAllAccounts());
    }
}

// login screen
class LoginFrame extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel statusLabel;

    private final BankSystem bankSystem;
    private final GuiState state;

    public LoginFrame(BankSystem bankSystem) {
        this(bankSystem, new GuiState());
    }

    public LoginFrame(BankSystem bankSystem, GuiState state) {
        this.bankSystem = bankSystem;
        this.state = state;

        setTitle("Bank Administration Panel - Login");
        setSize(560, 620);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.BG);
        setLayout(new BorderLayout());

        // banner
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(Theme.PRIMARY);
        banner.setBorder(new EmptyBorder(28, 30, 28, 30));

        JLabel bankName = new JLabel("APEX BANK");
        bankName.setFont(new Font("SansSerif", Font.BOLD, 22));
        bankName.setForeground(Color.WHITE);
        banner.add(bankName, BorderLayout.WEST);

        JLabel goldDot = new JLabel("\u25CF");
        goldDot.setForeground(Theme.ACCENT);
        goldDot.setFont(new Font("SansSerif", Font.BOLD, 28));
        banner.add(goldDot, BorderLayout.EAST);

        add(banner, BorderLayout.NORTH);

        // login card
        JPanel centerWrap = new JPanel(new GridBagLayout());
        centerWrap.setBackground(Theme.BG);

        JPanel card = Theme.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(420, 420));

        JLabel welcome = new JLabel("Welcome to the Bank Portal");
        welcome.setFont(Theme.FONT_TITLE);
        welcome.setForeground(Theme.PRIMARY);
        welcome.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(welcome);
        card.add(Box.createVerticalStrut(28));

        // full name
        JLabel uLabel = new JLabel("FULL NAME");
        uLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        uLabel.setForeground(Theme.SUBTEXT);
        uLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(uLabel);
        card.add(Box.createVerticalStrut(6));

        usernameField = new JTextField();
        Theme.styleField(usernameField);
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(usernameField);
        card.add(Box.createVerticalStrut(16));

        // password field
        JLabel pLabel = new JLabel("PASSWORD");
        pLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        pLabel.setForeground(Theme.SUBTEXT);
        pLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(pLabel);
        card.add(Box.createVerticalStrut(6));

        passwordField = new JPasswordField();
        Theme.styleField(passwordField);
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(passwordField);
        card.add(Box.createVerticalStrut(20));

        // login button
        JButton loginButton = Theme.primaryButton("Login");
        loginButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        card.add(loginButton);
        card.add(Box.createVerticalStrut(8));

        // sign-up button
        JButton signUpButton = Theme.secondaryButton("Create Account");
        signUpButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        signUpButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        card.add(signUpButton);
        card.add(Box.createVerticalStrut(8));

        // admin button
        JButton adminButton = Theme.secondaryButton("Admin Panel");
        adminButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        adminButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        card.add(adminButton);
        card.add(Box.createVerticalStrut(14));

        statusLabel = new JLabel(" ");
        statusLabel.setFont(Theme.FONT_LABEL);
        statusLabel.setForeground(Theme.DANGER);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(statusLabel);

        centerWrap.add(card);
        add(centerWrap, BorderLayout.CENTER);

        // footer
        JLabel footer = new JLabel("Secure banking, simplified.", SwingConstants.CENTER);
        footer.setFont(new Font("SansSerif", Font.ITALIC, 12));
        footer.setForeground(Theme.SUBTEXT);
        footer.setBorder(new EmptyBorder(0, 0, 16, 0));
        add(footer, BorderLayout.SOUTH);

        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleLogin();
            }
        });
        signUpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new CreateAccountFrame(bankSystem, state, null, null).setVisible(true);
                dispose();
            }
        });
        adminButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleAdminAccess();
            }
        });
    }

    private void handleLogin() {
        String fullName = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (fullName.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter your name and password.");
            return;
        }

        // check against the database
        User user = bankSystem.authenticateUser(fullName, password);

        if (user != null) {
            statusLabel.setText(" ");
            JOptionPane.showMessageDialog(this,
                    "Welcome back, " + user.getFullName() + "!",
                    "Login Success", JOptionPane.INFORMATION_MESSAGE);
            new DashboardFrame(bankSystem, state, user).setVisible(true);
            dispose();
        } else {
            statusLabel.setText("Invalid credentials. Check your name and password.");
        }
    }

    // admin login popup
    private void handleAdminAccess() {
        AdminLoginDialog dlg = new AdminLoginDialog(this);
        dlg.setVisible(true);
        if (!dlg.wasSubmitted())
            return;

        String adminUser = dlg.getEnteredUsername();
        String adminPass = dlg.getEnteredPassword();

        if (Main.ADMIN_USERNAME.equals(adminUser) && Main.ADMIN_PASSWORD.equals(adminPass)) {
            new AdminPanelFrame(bankSystem, state).setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Incorrect admin username or password.",
                    "Access Denied",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}

// admin login dialog
class AdminLoginDialog extends JDialog {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private boolean submitted = false;
    private String enteredUsername = "";
    private String enteredPassword = "";

    public AdminLoginDialog(JFrame parent) {
        super(parent, "Admin Login", true);
        setSize(440, 280);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(Theme.BG);
        setLayout(new BorderLayout());

        // header bar
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.PRIMARY);
        header.setBorder(new EmptyBorder(16, 22, 16, 22));
        JLabel title = new JLabel("Admin Access Required");
        title.setFont(Theme.FONT_HEADING);
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Theme.BG);
        form.setBorder(new EmptyBorder(20, 26, 16, 26));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 6, 8, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel ul = new JLabel("Admin Username:");
        ul.setFont(Theme.FONT_LABEL);
        form.add(ul, gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        usernameField = new JTextField();
        Theme.styleField(usernameField);
        form.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel pl = new JLabel("Admin Password:");
        pl.setFont(Theme.FONT_LABEL);
        form.add(pl, gbc);
        gbc.gridx = 1;
        gbc.gridy = 1;
        passwordField = new JPasswordField();
        Theme.styleField(passwordField);
        form.add(passwordField, gbc);

        add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttons.setBackground(Theme.BG);
        buttons.setBorder(new EmptyBorder(0, 16, 14, 16));
        JButton ok = Theme.primaryButton("Login");
        JButton cancel = Theme.secondaryButton("Cancel");
        buttons.add(cancel);
        buttons.add(ok);
        add(buttons, BorderLayout.SOUTH);

        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                submitted = true;
                enteredUsername = usernameField.getText().trim();
                enteredPassword = new String(passwordField.getPassword());
                dispose();
            }
        });
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                submitted = false;
                dispose();
            }
        });
    }

    public boolean wasSubmitted() {
        return submitted;
    }

    public String getEnteredUsername() {
        return enteredUsername;
    }

    public String getEnteredPassword() {
        return enteredPassword;
    }
}

// signup screen
class CreateAccountFrame extends JFrame {

    private JTextField usernameField;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JComboBox<String> accountTypeBox;
    private JTextField initialDepositField;
    private JLabel statusLabel;

    private final BankSystem bankSystem;
    private final GuiState state;

    public CreateAccountFrame(BankSystem bankSystem, GuiState state,
            String prefilledUsername, String prefilledPassword) {
        this.bankSystem = bankSystem;
        this.state = state;

        setTitle("Create New Account");
        setSize(620, 680);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.BG);
        setLayout(new BorderLayout());

        // banner
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(Theme.PRIMARY);
        banner.setBorder(new EmptyBorder(24, 30, 24, 30));
        JLabel title = new JLabel("Create Your Account");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        banner.add(title, BorderLayout.WEST);
        add(banner, BorderLayout.NORTH);

        // card
        JPanel centerWrap = new JPanel(new GridBagLayout());
        centerWrap.setBackground(Theme.BG);

        JPanel card = Theme.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(480, 580));

        JLabel heading = new JLabel("New Customer Registration");
        heading.setFont(Theme.FONT_HEADING);
        heading.setForeground(Theme.PRIMARY);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(heading);

        JPanel underline = new JPanel();
        underline.setBackground(Theme.ACCENT);
        underline.setMaximumSize(new Dimension(60, 3));
        underline.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(Box.createVerticalStrut(8));
        card.add(underline);
        card.add(Box.createVerticalStrut(20));

        // full name
        card.add(makeFieldLabel("FULL NAME"));
        card.add(Box.createVerticalStrut(6));
        usernameField = new JTextField();
        Theme.styleField(usernameField);
        if (prefilledUsername != null)
            usernameField.setText(prefilledUsername);
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(usernameField);
        card.add(Box.createVerticalStrut(14));

        // email
        card.add(makeFieldLabel("EMAIL"));
        card.add(Box.createVerticalStrut(6));
        emailField = new JTextField();
        Theme.styleField(emailField);
        emailField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        emailField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(emailField);
        card.add(Box.createVerticalStrut(14));

        // password
        card.add(makeFieldLabel("PASSWORD"));
        card.add(Box.createVerticalStrut(6));
        passwordField = new JPasswordField();
        Theme.styleField(passwordField);
        if (prefilledPassword != null)
            passwordField.setText(prefilledPassword);
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(passwordField);

        JLabel hint = new JLabel("8-16 characters, must include a special character");
        hint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        hint.setForeground(Theme.SUBTEXT);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(Box.createVerticalStrut(4));
        card.add(hint);
        card.add(Box.createVerticalStrut(14));

        // account type
        card.add(makeFieldLabel("ACCOUNT TYPE"));
        card.add(Box.createVerticalStrut(6));
        String[] accountTypes = { "CHECKING", "SAVINGS", "CD" };
        accountTypeBox = new JComboBox<String>(accountTypes);
        accountTypeBox.setFont(Theme.FONT_FIELD);
        accountTypeBox.setBackground(Color.WHITE);

        JPanel typeRow = new JPanel(new BorderLayout(8, 0));
        typeRow.setBackground(Theme.CARD);
        typeRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        typeRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        typeRow.add(accountTypeBox, BorderLayout.CENTER);
        typeRow.add(AccountTypeInfo.createInfoButton(this), BorderLayout.EAST);
        card.add(typeRow);
        card.add(Box.createVerticalStrut(14));

        // initial deposit
        card.add(makeFieldLabel("INITIAL DEPOSIT ($)"));
        card.add(Box.createVerticalStrut(6));
        initialDepositField = new JTextField();
        Theme.styleField(initialDepositField);
        MoneyFieldFormatter.attach(initialDepositField);
        initialDepositField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        initialDepositField.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(initialDepositField);
        card.add(Box.createVerticalStrut(20));

        // buttons
        JPanel btnRow = new JPanel();
        btnRow.setLayout(new BoxLayout(btnRow, BoxLayout.X_AXIS));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton createButton = Theme.primaryButton("Create Account");
        JButton backButton = Theme.secondaryButton("Back to Login");
        btnRow.add(createButton);
        btnRow.add(Box.createHorizontalStrut(10));
        btnRow.add(backButton);
        card.add(btnRow);
        card.add(Box.createVerticalStrut(12));

        statusLabel = new JLabel(" ");
        statusLabel.setFont(Theme.FONT_LABEL);
        statusLabel.setForeground(Theme.DANGER);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(statusLabel);

        centerWrap.add(card);
        add(centerWrap, BorderLayout.CENTER);

        createButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleCreate();
            }
        });
        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new LoginFrame(bankSystem, state).setVisible(true);
                dispose();
            }
        });
    }

    private JLabel makeFieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 11));
        l.setForeground(Theme.SUBTEXT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private void handleCreate() {
        String fullName = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        String accountType = (String) accountTypeBox.getSelectedItem();
        // strip commas so 10,000 works
        String depositStr = initialDepositField.getText().trim().replace(",", "");

        if (fullName.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please fill in your name and password.");
            return;
        }

        // auto generate email if left blank
        if (email.isEmpty()) {
            email = fullName.toLowerCase().replace(" ", ".") + "@apexbank.com";
        }

        // cd not implemented yet
        if ("CD".equals(accountType)) {
            JOptionPane.showMessageDialog(this,
                    "CD accounts are coming soon. Please choose CHECKING or SAVINGS for now.",
                    "Not Available", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // validate password
        PasswordValidate validator = new PasswordValidate();
        if (!validator.isStrong(password)) {
            statusLabel.setText("Password is not strong (8-16 chars + special character).");
            return;
        }

        // parse deposit amount
        BigDecimal depositAmount;
        if (depositStr.isEmpty()) {
            statusLabel.setText("Please enter an initial deposit amount.");
            return;
        }
        try {
            depositAmount = new BigDecimal(depositStr);
            if (depositAmount.compareTo(BigDecimal.ZERO) < 0) {
                statusLabel.setText("Deposit amount cannot be negative.");
                return;
            }
        } catch (NumberFormatException ex) {
            statusLabel.setText("Please enter a valid number for deposit.");
            return;
        }

        // register user in postgres
        int userId = bankSystem.registerUser(fullName, email, password);
        if (userId == -1) {
            JOptionPane.showMessageDialog(this,
                    "Registration failed.\nThe email may already be in use, or the database is unavailable.",
                    "Registration Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // create their first account
        int accountId = bankSystem.createAccount(userId, accountType, depositAmount);
        if (accountId == -1) {
            JOptionPane.showMessageDialog(this,
                    "User registered (ID: " + userId + "), but account creation failed.\n"
                            + "Please log in and create an account from the dashboard.",
                    "Partial Success", JOptionPane.WARNING_MESSAGE);
            new LoginFrame(bankSystem, state).setVisible(true);
            dispose();
            return;
        }

        // success - send back to login

        JOptionPane.showMessageDialog(this,
                "Account created successfully!\n"
                        + "Name: " + fullName + "\n"
                        + "Email: " + email + "\n"
                        + "Account Type: " + accountType + "\n"
                        + "Initial Deposit: $" + depositAmount + "\n\n"
                        + "Please log in with your credentials.",
                "Welcome to Apex Bank", JOptionPane.INFORMATION_MESSAGE);

        // go back to login
        new LoginFrame(bankSystem, state).setVisible(true);
        dispose();
    }
}

// dashboard after login
class DashboardFrame extends JFrame {

    private final BankSystem bankSystem;
    private final GuiState state;
    private final User currentUser;

    private JLabel welcomeLabel;
    private JLabel totalBalanceLabel;
    private DefaultListModel<String> accountListModel;
    private JList<String> accountList;

    public DashboardFrame(BankSystem bankSystem, GuiState state, User user) {
        this.bankSystem = bankSystem;
        this.state = state;
        this.currentUser = user;

        setTitle("Bank Dashboard - " + user.getUsername());
        setSize(960, 660);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.BG);
        setLayout(new BorderLayout());

        // banner with name and balance
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(Theme.PRIMARY);
        banner.setBorder(new EmptyBorder(24, 30, 24, 30));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);
        JLabel hi = new JLabel("Welcome,");
        hi.setFont(new Font("SansSerif", Font.PLAIN, 14));
        hi.setForeground(new Color(200, 210, 230));
        welcomeLabel = new JLabel(user.getUsername());
        welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        welcomeLabel.setForeground(Color.WHITE);
        left.add(hi);
        left.add(welcomeLabel);
        banner.add(left, BorderLayout.WEST);

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setOpaque(false);
        JLabel balLabel = new JLabel("TOTAL BALANCE");
        balLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        balLabel.setForeground(Theme.ACCENT);
        balLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        totalBalanceLabel = new JLabel("$0.00");
        totalBalanceLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        totalBalanceLabel.setForeground(Color.WHITE);
        totalBalanceLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        right.add(balLabel);
        right.add(totalBalanceLabel);
        banner.add(right, BorderLayout.EAST);
        add(banner, BorderLayout.NORTH);

        // sidebar buttons
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(Theme.PRIMARY_DARK);
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBorder(new EmptyBorder(20, 0, 20, 0));

        JLabel actionsTitle = new JLabel("  ACTIONS");
        actionsTitle.setFont(new Font("SansSerif", Font.BOLD, 11));
        actionsTitle.setForeground(Theme.ACCENT);
        actionsTitle.setBorder(new EmptyBorder(0, 12, 8, 0));
        sidebar.add(actionsTitle);

        JButton depositBtn = Theme.sidebarButton("\u2193  Deposit");
        JButton withdrawBtn = Theme.sidebarButton("\u2191  Withdraw");
        JButton transferBtn = Theme.sidebarButton("\u21C4  Transfer");
        JButton newAccountBtn = Theme.sidebarButton("\u002B  Make New Account");
        JButton howMuchBtn = Theme.sidebarButton("\u0024  Balance Inquiry");
        JButton applyInterestBtn = Theme.sidebarButton("\u0025  Apply Interest");
        JButton logoutBtn = Theme.sidebarButton("\u2192  Logout");

        // make buttons fill the sidebar width
        Dimension btnSize = new Dimension(220, 44);
        JButton[] sideButtons = { depositBtn, withdrawBtn, transferBtn, newAccountBtn,
                howMuchBtn, applyInterestBtn, logoutBtn };
        for (JButton b : sideButtons) {
            b.setMaximumSize(btnSize);
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            sidebar.add(b);
            sidebar.add(Box.createVerticalStrut(2));
        }
        sidebar.add(Box.createVerticalGlue());
        add(sidebar, BorderLayout.WEST);

        // account list area
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Theme.BG);
        content.setBorder(new EmptyBorder(24, 24, 24, 24));

        JPanel listCard = Theme.card();
        listCard.setLayout(new BorderLayout());

        JPanel listHeader = new JPanel(new BorderLayout());
        listHeader.setOpaque(false);
        listHeader.setBorder(new EmptyBorder(0, 0, 14, 0));
        JLabel listTitle = new JLabel("Your Accounts");
        listTitle.setFont(Theme.FONT_HEADING);
        listTitle.setForeground(Theme.PRIMARY);
        listHeader.add(listTitle, BorderLayout.WEST);
        listCard.add(listHeader, BorderLayout.NORTH);

        accountListModel = new DefaultListModel<String>();
        accountList = new JList<String>(accountListModel);
        accountList.setFont(Theme.FONT_MONO);
        accountList.setFixedCellHeight(38);
        accountList.setBackground(new Color(250, 251, 253));
        accountList.setBorder(new EmptyBorder(6, 6, 6, 6));
        JScrollPane scrollPane = new JScrollPane(accountList);
        scrollPane.setBorder(new LineBorder(new Color(225, 230, 240), 1));
        listCard.add(scrollPane, BorderLayout.CENTER);

        content.add(listCard, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        depositBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleDeposit();
            }
        });
        withdrawBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleWithdraw();
            }
        });
        transferBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleTransfer();
            }
        });
        newAccountBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleNewAccount();
            }
        });
        howMuchBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleHowMuch();
            }
        });
        applyInterestBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleApplyInterest();
            }
        });
        logoutBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                state.markOffline(currentUser.getUsername());
                new LoginFrame(bankSystem, state).setVisible(true);
                dispose();
            }
        });

        refreshAccountList();
    }

    // refresh the account list ui
    private void refreshAccountList() {
        accountListModel.clear();
        List<Account> accounts = state.getAccountsForUser(currentUser.getUserId());
        if (accounts.isEmpty()) {
            accountListModel.addElement("  No accounts yet. Click \"Make New Account\" to create one.");
        } else {
            for (Account acc : accounts) {
                String type = "ACCOUNT";
                if (acc instanceof CheckingAccount)
                    type = "CHECKING";
                else if (acc instanceof SavingsAccount)
                    type = "SAVINGS";
                accountListModel.addElement(
                        String.format("  [%-8s]   Account #%d        Balance:  $%s",
                                type, acc.getAccountId(), acc.getBalance().toString()));
            }
        }
        BigDecimal total = new Admin().calculateTotalBalance(accounts);
        totalBalanceLabel.setText("$" + total.toString());
    }

    // lets user pick an account from a dropdown
    private Account selectAccount(String prompt) {
        List<Account> accounts = state.getAccountsForUser(currentUser.getUserId());
        if (accounts.isEmpty()) {
            JOptionPane.showMessageDialog(this, "You have no accounts yet.");
            return null;
        }
        String[] labels = new String[accounts.size()];
        for (int i = 0; i < accounts.size(); i++) {
            Account a = accounts.get(i);
            String type = (a instanceof CheckingAccount) ? "CHECKING"
                    : (a instanceof SavingsAccount) ? "SAVINGS" : "ACCOUNT";
            labels[i] = type + " #" + a.getAccountId() + "  ($" + a.getBalance() + ")";
        }
        String chosen = (String) JOptionPane.showInputDialog(this,
                prompt, "Select Account",
                JOptionPane.PLAIN_MESSAGE, null, labels, labels[0]);
        if (chosen == null)
            return null;
        for (int i = 0; i < labels.length; i++) {
            if (labels[i].equals(chosen))
                return accounts.get(i);
        }
        return null;
    }

    // ask user for a dollar amount
    private BigDecimal askForAmount(String prompt) {
        String input = JOptionPane.showInputDialog(this, prompt, "Amount", JOptionPane.PLAIN_MESSAGE);
        if (input == null)
            return null;
        try {
            // strip commas
            BigDecimal amt = new BigDecimal(input.trim().replace(",", ""));
            if (amt.compareTo(BigDecimal.ZERO) <= 0) {
                JOptionPane.showMessageDialog(this, "Amount must be positive.");
                return null;
            }
            return amt;
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid number.");
            return null;
        }
    }

    private void handleDeposit() {
        Account acc = selectAccount("Choose an account to deposit into:");
        if (acc == null)
            return;
        BigDecimal amt = askForAmount("Enter amount to deposit:");
        if (amt == null)
            return;

        // flag large transactions
        Admin admin = new Admin();
        if (admin.isSuspicious(acc, amt)) {
            JOptionPane.showMessageDialog(this,
                    "Notice: this transaction is over $10,000 and will be reported.",
                    "Large Transaction", JOptionPane.WARNING_MESSAGE);
        }

        boolean ok = bankSystem.deposit(acc, amt);
        JOptionPane.showMessageDialog(this,
                ok ? "Deposit successful!" : "Deposit failed.");
        refreshAccountList();
    }

    private void handleWithdraw() {
        Account acc = selectAccount("Choose an account to withdraw from:");
        if (acc == null)
            return;
        BigDecimal amt = askForAmount("Enter amount to withdraw:");
        if (amt == null)
            return;
        boolean ok = bankSystem.withdraw(acc, amt);
        JOptionPane.showMessageDialog(this,
                ok ? "Withdrawal successful!" : "Withdrawal failed (insufficient funds or limit).");
        refreshAccountList();
    }

    private void handleTransfer() {
        Account from = selectAccount("Choose the SOURCE account:");
        if (from == null)
            return;
        Account to = selectAccount("Choose the DESTINATION account:");
        if (to == null)
            return;
        if (from == to) {
            JOptionPane.showMessageDialog(this, "Source and destination cannot be the same.");
            return;
        }
        BigDecimal amt = askForAmount("Enter amount to transfer:");
        if (amt == null)
            return;
        boolean ok = bankSystem.transfer(from, to, amt);
        JOptionPane.showMessageDialog(this,
                ok ? "Transfer successful!" : "Transfer failed.");
        refreshAccountList();
    }

    private void handleNewAccount() {
        String[] types = { "CHECKING", "SAVINGS", "CD" };
        JComboBox<String> typeBox = new JComboBox<String>(types);
        JPanel typePanel = new JPanel(new BorderLayout(8, 0));
        typePanel.add(new JLabel("Account Type: "), BorderLayout.WEST);
        typePanel.add(typeBox, BorderLayout.CENTER);
        typePanel.add(AccountTypeInfo.createInfoButton(this), BorderLayout.EAST);

        int ok = JOptionPane.showConfirmDialog(this, typePanel,
                "New Account", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION)
            return;
        String type = (String) typeBox.getSelectedItem();
        if (type == null)
            return;

        if ("CD".equals(type)) {
            JOptionPane.showMessageDialog(this,
                    "CD accounts are coming soon. Please choose CHECKING or SAVINGS for now.",
                    "Not Available", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        BigDecimal initial = askForAmount("Enter initial deposit amount:");
        if (initial == null)
            return;

        int id = bankSystem.createAccount(currentUser.getUserId(), type, initial);
        if (id == -1) {
            JOptionPane.showMessageDialog(this, "Failed to create account.");
            return;
        }

        Account account;
        if ("CHECKING".equals(type)) {
            account = new CheckingAccount(id, currentUser.getUserId(), initial);
        } else {
            account = new SavingsAccount(id, currentUser.getUserId(), initial);
        }
        state.addAccount(currentUser.getUserId(), account);

        JOptionPane.showMessageDialog(this,
                type + " account created (#" + id + ") with $" + initial);
        refreshAccountList();
    }

    private void handleHowMuch() {
        Account acc = selectAccount("Which account's balance?");
        if (acc == null)
            return;
        BigDecimal bal = bankSystem.getBalance(acc);
        JOptionPane.showMessageDialog(this,
                "Balance for account #" + acc.getAccountId() + ": $" + bal,
                "Balance", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleApplyInterest() {
        Account acc = selectAccount("Apply interest to which account?");
        if (acc == null)
            return;
        // only savings gets interest
        if (!(acc instanceof SavingsAccount)) {
            JOptionPane.showMessageDialog(this,
                    "Interest can only be applied to Savings accounts.");
            return;
        }
        boolean ok = bankSystem.applyInterest(acc);
        JOptionPane.showMessageDialog(this,
                ok ? "Interest applied. New balance: $" + acc.getBalance() : "Failed to apply interest.");
        refreshAccountList();
    }
}

// admin panel
class AdminPanelFrame extends JFrame {

    private final BankSystem bankSystem;
    private final GuiState state;

    private JLabel totalUsersValue;
    private JLabel onlineUsersValue;
    private JLabel totalAccountsValue;
    private JLabel checkingCountValue;
    private JLabel savingsCountValue;
    private JLabel totalMoneyValue;

    private DefaultListModel<String> userListModel;
    private JList<String> userList;

    public AdminPanelFrame(BankSystem bankSystem, GuiState state) {
        this.bankSystem = bankSystem;
        this.state = state;

        setTitle("Admin Panel");
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.BG);
        setLayout(new BorderLayout());

        // banner
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(Theme.PRIMARY_DARK);
        banner.setBorder(new EmptyBorder(24, 30, 24, 30));
        JLabel title = new JLabel("Admin Control Panel");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        JLabel sub = new JLabel("System statistics & user overview");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sub.setForeground(Theme.ACCENT);

        JPanel bannerLeft = new JPanel();
        bannerLeft.setOpaque(false);
        bannerLeft.setLayout(new BoxLayout(bannerLeft, BoxLayout.Y_AXIS));
        bannerLeft.add(title);
        bannerLeft.add(Box.createVerticalStrut(2));
        bannerLeft.add(sub);
        banner.add(bannerLeft, BorderLayout.WEST);
        add(banner, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBackground(Theme.BG);
        content.setBorder(new EmptyBorder(20, 24, 20, 24));

        // stat cards grid
        JPanel statsGrid = new JPanel(new GridLayout(2, 3, 14, 14));
        statsGrid.setBackground(Theme.BG);

        totalUsersValue = new JLabel("0");
        onlineUsersValue = new JLabel("0");
        totalAccountsValue = new JLabel("0");
        totalMoneyValue = new JLabel("$0.00");
        checkingCountValue = new JLabel("0");
        savingsCountValue = new JLabel("0");

        statsGrid.add(makeStatCard("Total Users", totalUsersValue, Theme.PRIMARY));
        statsGrid.add(makeStatCard("Users Online", onlineUsersValue, Theme.SUCCESS));
        statsGrid.add(makeStatCard("Total Accounts", totalAccountsValue, Theme.PRIMARY));
        statsGrid.add(makeStatCard("Money in Bank", totalMoneyValue, Theme.ACCENT));
        statsGrid.add(makeStatCard("Checking Accts", checkingCountValue, Theme.PRIMARY));
        statsGrid.add(makeStatCard("Savings Accts", savingsCountValue, Theme.PRIMARY));

        content.add(statsGrid, BorderLayout.NORTH);

        // user list
        JPanel listCard = Theme.card();
        listCard.setLayout(new BorderLayout());

        JLabel listTitle = new JLabel("All Users & Accounts");
        listTitle.setFont(Theme.FONT_HEADING);
        listTitle.setForeground(Theme.PRIMARY);
        listTitle.setBorder(new EmptyBorder(0, 0, 12, 0));
        listCard.add(listTitle, BorderLayout.NORTH);

        userListModel = new DefaultListModel<String>();
        userList = new JList<String>(userListModel);
        userList.setFont(Theme.FONT_MONO);
        userList.setFixedCellHeight(32);
        userList.setBackground(new Color(250, 251, 253));
        userList.setBorder(new EmptyBorder(6, 6, 6, 6));
        JScrollPane scroll = new JScrollPane(userList);
        scroll.setBorder(new LineBorder(new Color(225, 230, 240), 1));
        listCard.add(scroll, BorderLayout.CENTER);

        content.add(listCard, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        // buttons at the bottom
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        buttonPanel.setBackground(Theme.BG);
        buttonPanel.setBorder(new EmptyBorder(0, 24, 14, 24));
        JButton refreshBtn = Theme.primaryButton("Refresh");
        JButton closeBtn = Theme.secondaryButton("Close");
        buttonPanel.add(closeBtn);
        buttonPanel.add(refreshBtn);
        add(buttonPanel, BorderLayout.SOUTH);

        refreshBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                refreshStats();
            }
        });
        closeBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        refreshStats();
    }

    // builds one stat card widget
    private JPanel makeStatCard(String label, JLabel valueLabel, Color accent) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Theme.CARD);
        p.setBorder(new CompoundBorder(
                new MatteBorder(0, 4, 0, 0, accent),
                new CompoundBorder(
                        new LineBorder(new Color(225, 230, 240), 1),
                        new EmptyBorder(14, 16, 14, 16))));

        JLabel l = new JLabel(label.toUpperCase());
        l.setFont(new Font("SansSerif", Font.BOLD, 11));
        l.setForeground(Theme.SUBTEXT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        valueLabel.setForeground(Theme.PRIMARY);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(l);
        p.add(Box.createVerticalStrut(6));
        p.add(valueLabel);
        return p;
    }

    // updates all the stat labels and user list
    private void refreshStats() {
        List<User> allUsers = state.getAllUsers();
        List<Account> allAccounts = state.getAllAccounts();

        totalUsersValue.setText(String.valueOf(allUsers.size()));
        onlineUsersValue.setText(String.valueOf(state.getOnlineUserCount()));
        totalAccountsValue.setText(String.valueOf(allAccounts.size()));
        checkingCountValue.setText(String.valueOf(state.countCheckingAccounts()));
        savingsCountValue.setText(String.valueOf(state.countSavingsAccounts()));
        totalMoneyValue.setText("$" + state.getTotalMoneyInBank());

        userListModel.clear();
        if (allUsers.isEmpty()) {
            userListModel.addElement("  (no users yet)");
        } else {
            Admin admin = new Admin();
            for (User u : allUsers) {
                List<Account> ua = state.getAccountsForUser(u.getUserId());
                BigDecimal userTotal = admin.calculateTotalBalance(ua);
                userListModel.addElement(
                        String.format("  User #%d   %-15s   accounts: %d    total: $%s",
                                u.getUserId(), u.getUsername(), ua.size(), userTotal));
            }
        }
    }
}

// info button that explains account types
class AccountTypeInfo {

    private static final String DESCRIPTION = "<html><body style='width: 340px; font-family: SansSerif; font-size: 12px;'>"
            + "<h3 style='margin-bottom:4px; color:#142D5A;'>Account Types</h3>"
            + "<p><b>CHECKING</b><br>"
            + "An everyday account for daily spending and bill payments. "
            + "Allows overdraft up to $500. Best if you make frequent deposits and withdrawals.</p>"
            + "<p><b>SAVINGS</b><br>"
            + "An account for storing money long-term. Earns 3% interest. "
            + "No overdraft allowed. Best if you want your money to grow.</p>"
            + "<p><b>CD</b> (Certificate of Deposit)<br>"
            + "A fixed-term account that locks your money for a set period in exchange "
            + "for a higher interest rate. Early withdrawal usually has a penalty. "
            + "Best for money you don't need to touch right away.</p>"
            + "</body></html>";

    public static JButton createInfoButton(final Component parent) {
        JButton btn = new JButton("?");
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setBackground(Theme.PRIMARY);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(true);
        btn.setMargin(new Insets(0, 0, 0, 0));
        btn.setPreferredSize(new Dimension(28, 28));
        btn.setToolTipText("What do these account types mean?");
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(Theme.ACCENT);
                btn.setForeground(Theme.PRIMARY_DARK);
            }

            public void mouseExited(MouseEvent e) {
                btn.setBackground(Theme.PRIMARY);
                btn.setForeground(Color.WHITE);
            }
        });
        btn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(parent,
                        DESCRIPTION,
                        "About Account Types",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });
        return btn;
    }
}

// formats money fields with commas as you type
class MoneyFieldFormatter {

    public static void attach(final javax.swing.JTextField field) {
        field.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {

            // flag so we dont loop while updating
            private boolean updating = false;

            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                reformat();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                reformat();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
            }

            private void reformat() {
                if (updating)
                    return;
                final String raw = field.getText();
                final String cleaned = stripToNumber(raw);
                final String formatted = addCommas(cleaned);
                if (!formatted.equals(raw)) {
                    javax.swing.SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            updating = true;
                            field.setText(formatted);
                            updating = false;
                        }
                    });
                }
            }
        });
    }

    // strips out everything except digits and one decimal
    private static String stripToNumber(String s) {
        StringBuilder sb = new StringBuilder();
        boolean dotSeen = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isDigit(c)) {
                sb.append(c);
            } else if (c == '.' && !dotSeen) {
                sb.append(c);
                dotSeen = true;
            }
        }
        return sb.toString();
    }

    // adds commas to the number part
    private static String addCommas(String s) {
        if (s == null || s.isEmpty())
            return "";
        int dot = s.indexOf('.');
        String intPart = (dot >= 0) ? s.substring(0, dot) : s;
        String fracPart = (dot >= 0) ? s.substring(dot) : "";
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = intPart.length() - 1; i >= 0; i--) {
            sb.insert(0, intPart.charAt(i));
            count++;
            if (count == 3 && i > 0) {
                sb.insert(0, ',');
                count = 0;
            }
        }
        return sb.toString() + fracPart;
    }
}
