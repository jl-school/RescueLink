package com.rescuelink.ui;

import com.rescuelink.dao.DataAccessException;
import com.rescuelink.model.User;
import com.rescuelink.service.UserService;
import com.rescuelink.util.DateTimeUtil;

/** Home screen shown after login. Logging out returns to the main menu (it does not exit the program). */
public class UserMenu {

    private final UserService userService;
    private final UserManagementMenu userManagementMenu;

    public UserMenu(UserService userService, UserManagementMenu userManagementMenu) {
        this.userService = userService;
        this.userManagementMenu = userManagementMenu;
    }

    public void run(User user) {
        while (true) {
            TerminalUi.banner("YOUR RESCUELINK", user.fullName() + "  ·  " + user.role().getDisplayName());
            System.out.println("  " + user.role().getDescription());
            System.out.println("  Role modules for households, incidents and relief are coming soon.");
            System.out.println();
            System.out.println("  1  View my profile");
            if (userService.canManageUsers(user)) {
                System.out.println("  2  Manage accounts");
            }
            System.out.println("  0  Sign out");
            int choice = ConsoleInput.readInt("  Select an option: ", 0, 2);
            try {
                switch (choice) {
                    case 1 -> showProfile(user);
                    case 2 -> {
                        if (userService.canManageUsers(user)) {
                            userManagementMenu.run(user);
                        } else {
                            System.out.println("That option is not available for your role.");
                        }
                    }
                    default -> {
                        userService.logout(user);
                        System.out.println("You have been logged out.");
                        return;
                    }
                }
            } catch (DataAccessException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private void showProfile(User user) {
        TerminalUi.banner("YOUR PROFILE", "Account details and membership");
        System.out.printf("  %-14s %s%n", "Member ID", "RL-" + String.format("%05d", user.userId()));
        System.out.printf("  %-14s %s%n", "Full name", user.fullName());
        System.out.printf("  %-14s @%s%n", "Username", user.username());
        System.out.printf("  %-14s %s%n", "Role", user.role().getDisplayName());
        System.out.printf("  %-14s %s%n", "Contact", user.contactDetails() == null || user.contactDetails().isBlank()
                ? "Not provided" : user.contactDetails());
        System.out.printf("  %-14s %s%n", "Account", TerminalUi.status(user.accountStatus().name()));
        System.out.printf("  %-14s %s%n", "Member since", DateTimeUtil.format(user.createdAt()));
        System.out.println("\n  Thank you for being part of your local response network.");
    }
}
