package com.rescuelink.ui;

import com.rescuelink.dao.DataAccessException;
import com.rescuelink.model.User;
import com.rescuelink.service.UserService;

/** The menu shown before login: Register, Login, Exit. */
public class MainMenu {

    private final UserService userService;
    private final AuthenticationMenu authenticationMenu;
    private final UserMenu userMenu;

    public MainMenu(UserService userService, AuthenticationMenu authenticationMenu, UserMenu userMenu) {
        this.userService = userService;
        this.authenticationMenu = authenticationMenu;
        this.userMenu = userMenu;
    }

    public void run() {
        TerminalUi.banner("COMMUNITY RESPONSE & RELIEF", "Connected communities. Coordinated response.");
        System.out.println("  For emergencies, contact local emergency services.");

        try {
            if (userService.needsFirstAdministrator() && !authenticationMenu.runFirstAdministratorSetup()) {
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
            int choice = ConsoleInput.readInt("  Select an option: ", 0, 2);
            try {
                switch (choice) {
                    case 1 -> authenticationMenu.register();
                    case 2 -> {
                        User user = authenticationMenu.login();
                        if (user != null) {
                            userMenu.run(user);
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
}
