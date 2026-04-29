# Bank Administration Panel

A Java-based banking administration application that allows users and administrators to manage bank accounts through a desktop GUI. The project uses object-oriented programming concepts with classes such as `User`, `Admin`, `Account`, `CheckingAccount`, and `SavingsAccount`.

## Features

- User login and account registration
- Admin and user access levels
- Checking and savings account support
- Deposit, withdrawal, and transfer transactions
- Balance inquiry
- Savings account interest application
- Checking account overdraft limit
- Large transaction warning for amounts over $10,000
- Password validation
- Database-backed account and user management
- Java Swing graphical interface

## Technologies Used

- Java
- Java Swing
- Object-Oriented Programming
- BigDecimal for currency handling
- Database integration through `DatabaseManager`

## Project Structure

```text
Bank-Administration-Panel/
├── Account.java
├── Admin.java
├── BankSystem.java
├── CheckingAccount.java
├── DatabaseManager.java
├── Main.java
├── PasswordUtil.java
├── PasswordValidate.java
├── SavingsAccount.java
├── User.java
├── database/
└── lib/

## HOW TO RUN

All a user would need to do to run this repo is click on Main.java and run it will automatically connect to our backend and save user info if you ever want to retrive it back always log back in the same device or a diffrent one.