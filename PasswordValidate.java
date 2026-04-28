public class PasswordValidate {
    // to be a valid password the length needs to be in between 8 - 16 and contain
    // one special character
    private char[] specialChars = {
            '!', '@', '#', '$', '%', '^', '&', '*',
            '(', ')', '-', '_', '=', '+',
            '[', ']', '{', '}', ';', ':',
            '\'', '"', ',', '.', '<', '>',
            '/', '?', '|', '`', '~'
    };

    public boolean isStrong(String password) {
        if (password == null) {
            System.out.println("Password cannot be null!");
            return false;
        }

        if (password.length() < 8 || password.length() > 16) {
            System.out.println("Password does not meet the length requirements!");
            return false;
        }

        boolean hasSpecial = false;

        for (char c : specialChars) {
            if (password.indexOf(c) != -1) {
                hasSpecial = true;
                break;
            }
        }

        if (!hasSpecial) {
            System.out.println("Password must contain a special character!");
            return false;
        }

        return true;
    }
}