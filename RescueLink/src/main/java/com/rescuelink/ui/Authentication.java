package com.rescuelink.ui;

import com.rescuelink.dao.ActivityLogDao;
import com.rescuelink.dao.DataAccessException;
import com.rescuelink.dao.UserDao;
import com.rescuelink.model.Role;
import com.rescuelink.model.User;
import com.rescuelink.service.AuthException;
import com.rescuelink.service.UserService;

import java.util.Scanner;

/**
 * The only place that handles first-run setup, registration, login, and role routing.
 * Main creates one UserDao, one ActivityLogDao, and one Scanner and hands them in here;
 * they are then shared by every menu, so nothing creates a duplicate resource.
 */
public class Authentication {

    private final UserService userService;
    private final ConsoleInput input;
    private final String version;

    private final AdministratorMenu administratorMenu;
    private final ResponsePersonnelMenu responsePersonnelMenu;
    private final ReliefCoordinatorMenu reliefCoordinatorMenu;
    private final ResidentMenu residentMenu;

    public Authentication(UserDao userDao, ActivityLogDao activityLogDao, Scanner scanner, String version) {
        this.input = new ConsoleInput(scanner);
        this.userService = new UserService(userDao, activityLogDao);
        this.version = version;
        this.administratorMenu = new AdministratorMenu(userService, input);
        this.responsePersonnelMenu = new ResponsePersonnelMenu(userService, input);
        this.reliefCoordinatorMenu = new ReliefCoordinatorMenu(userService, input);
        this.residentMenu = new ResidentMenu(userService, input);
    }

    /** Startup flow: first-run setup if needed, then Register / Login / Exit until the user exits. */
    public void start() {
        TerminalUi.banner("COMMUNITY RESPONSE & RELIEF v" + version, "Connected communities. Coordinated response.");
        System.out.println("  For emergencies, contact local emergency services.");

        try {
            if (userService.needsFirstAdministrator() && !runFirstAdministratorSetup()) {
                System.out.println("No administrator exists yet. New accounts cannot be approved until one is created.");
            }
        } catch (DataAccessException e) {
            System.out.println("Database error: " + e.getMessage());
        }

        boolean running = true;
        while (running) {
            TerminalUi.section("WELCOME");
            System.out.println("  1  Create an account");
            System.out.println("  2  Sign in");
            System.out.println("  0  Exit RescueLink");
            int choice = input.readInt("  Select an option: ", 0, 2);
            try {
                switch (choice) {
                    case 1 -> register();
                    case 2 -> {
                        User user = login();
                        if (user != null) {
                            routeToRoleMenu(user);
                        }
                    }
                    default -> running = false;
                }
            } catch (DataAccessException e) {
                System.out.println("A database error occurred: " + e.getMessage());
            }
        }
        System.out.println();
        System.out.println("Thank you for supporting a more prepared community. Goodbye.");
    }

    /** Sends a logged-in user to the menu that matches their assigned role. */
    private void routeToRoleMenu(User user) {
        switch (user.role()) {
            case ADMINISTRATOR -> administratorMenu.run(user);
            case RESPONSE_PERSONNEL -> responsePersonnelMenu.run(user);
            case RELIEF_COORDINATOR -> reliefCoordinatorMenu.run(user);
            case RESIDENT -> residentMenu.run(user);
        }
    }

    // ------------------------------------------------------------ first-run setup

    /** Runs when no active administrator exists. Returns true if an administrator was created. */
    private boolean runFirstAdministratorSetup() {
        TerminalUi.banner("WELCOME SETUP", "Create the administrator account to get started");
        while (true) {
            String fullName = input.readLine("  Full name: ");
            String username = input.readLine("  Username: ");
            String password = input.readPassword("  Password: ");
            String confirm = input.readPassword("  Confirm password: ");
            String contact = input.readLine("  Contact details (optional): ");
            try {
                if (!password.equals(confirm)) {
                    throw new AuthException("Passwords do not match.");
                }
                User admin = userService.createFirstAdministrator(fullName, username, password, contact);
                System.out.println("Administrator account '" + admin.username() + "' created and activated.");
                return true;
            } catch (AuthException e) {
                System.out.println("Setup failed: " + e.getMessage());
                if (!input.readYesNo("Try again?")) {
                    return false;
                }
            }
        }
    }

    // --------------------------------------------------------------- registration

    private void register() {
        TerminalUi.banner("CREATE ACCOUNT", "Choose how you participate in community response");
        Role[] roles = Role.values();
        for (int i = 0; i < roles.length; i++) {
            System.out.println("  " + (i + 1) + "  " + roles[i].getDisplayName());
        }
        System.out.println("  0  Cancel");
        int choice = input.readInt("  Select a role: ", 0, roles.length);
        if (choice == 0) {
            return;
        }
        Role role = roles[choice - 1];

        TerminalUi.section("ACCOUNT DETAILS");
        String fullName = input.readLine("  Full name: ");
        String username = input.readLine("  Username (4-20 letters/numbers/underscore): ");
        String password = input.readPassword("  Password (min 8 chars, letters and numbers): ");
        String confirm = input.readPassword("  Confirm password: ");
        String contact = input.readLine("  Contact details (optional): ");
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

    // ---------------------------------------------------------------------- login

    /** Returns the logged-in user, or null if login failed. */
    private User login() {
        TerminalUi.banner("SIGN IN", "Welcome back to your community response network");
        String username = input.readLine("  Username: ");
        String password = input.readPassword("  Password: ");
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
