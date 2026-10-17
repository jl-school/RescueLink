package com.rescuelink.ui;

import com.rescuelink.model.Role;
import com.rescuelink.model.User;
import com.rescuelink.service.AuthException;
import com.rescuelink.service.UserService;

/** Screens for first-run administrator setup, account registration, and login. */
public class AuthenticationMenu {

    private final UserService userService;

    public AuthenticationMenu(UserService userService) {
        this.userService = userService;
    }

    /** Runs when no active administrator exists. Returns true if an administrator was created. */
    public boolean runFirstAdministratorSetup() {
        TerminalUi.banner("WELCOME SETUP", "Create the administrator account to get started");
        while (true) {
            String fullName = ConsoleInput.readLine("  Full name: ");
            String username = ConsoleInput.readLine("  Username: ");
            String password = ConsoleInput.readPassword("  Password: ");
            String confirm = ConsoleInput.readPassword("  Confirm password: ");
            String contact = ConsoleInput.readLine("  Contact details (optional): ");
            try {
                if (!password.equals(confirm)) {
                    throw new AuthException("Passwords do not match.");
                }
                User admin = userService.createFirstAdministrator(fullName, username, password, contact);
                System.out.println("Administrator account '" + admin.username() + "' created and activated.");
                return true;
            } catch (AuthException e) {
                System.out.println("Setup failed: " + e.getMessage());
                if (!ConsoleInput.readYesNo("Try again?")) {
                    return false;
                }
            }
        }
    }

    public void register() {
        TerminalUi.banner("CREATE ACCOUNT", "Choose how you participate in community response");
        System.out.println("  1  " + Role.RESPONSE_PERSONNEL.getDisplayName());
        System.out.println("     " + Role.RESPONSE_PERSONNEL.getDescription());
        System.out.println("  2  " + Role.RELIEF_COORDINATOR.getDisplayName());
        System.out.println("     " + Role.RELIEF_COORDINATOR.getDescription());
        System.out.println("  3  " + Role.RESIDENT.getDisplayName());
        System.out.println("     " + Role.RESIDENT.getDescription());
        System.out.println("  0  Cancel");
        int choice = ConsoleInput.readInt("  Select a role: ", 0, 3);
        if (choice == 0) {
            return;
        }
        Role role = switch (choice) {
            case 1 -> Role.RESPONSE_PERSONNEL;
            case 2 -> Role.RELIEF_COORDINATOR;
            default -> Role.RESIDENT;
        };

        TerminalUi.section("ACCOUNT DETAILS");
        String fullName = ConsoleInput.readLine("  Full name: ");
        String username = ConsoleInput.readLine("  Username (4-20 letters/numbers/underscore): ");
        String password = ConsoleInput.readPassword("  Password (min 8 chars, letters and numbers): ");
        String confirm = ConsoleInput.readPassword("  Confirm password: ");
        String contact = ConsoleInput.readLine("  Contact details (optional): ");
        try {
            if (!password.equals(confirm)) {
                throw new AuthException("Passwords do not match.");
            }
            User user = userService.register(fullName, username, password, role, contact);
            TerminalUi.section("REGISTRATION RECEIVED");
            System.out.println("  Welcome, " + user.fullName() + ". Your account is " + TerminalUi.status("PENDING") + ".");
            System.out.println("  An administrator will review your account before you can sign in.");
        } catch (AuthException e) {
            System.out.println("Registration failed: " + e.getMessage());
        }
    }

    /** Returns the logged-in user, or null if login failed. */
    public User login() {
        TerminalUi.banner("SIGN IN", "Welcome back to your community response network");
        String username = ConsoleInput.readLine("  Username: ");
        String password = ConsoleInput.readPassword("  Password: ");
        try {
            User user = userService.login(username, password);
            System.out.println("Welcome back, " + user.fullName() + ".");
            return user;
        } catch (AuthException e) {
            System.out.println("Login failed: " + e.getMessage());
            return null;
        }
    }
}
