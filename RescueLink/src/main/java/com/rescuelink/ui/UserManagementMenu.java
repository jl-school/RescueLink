package com.rescuelink.ui;

import com.rescuelink.dao.DataAccessException;
import com.rescuelink.model.AccessDeniedException;
import com.rescuelink.model.AccountStatus;
import com.rescuelink.model.User;
import com.rescuelink.service.UserService;
import com.rescuelink.util.DateTimeUtil;

import java.util.List;

/** Administrator-only screens for approving and deactivating accounts. */
public class UserManagementMenu {

    private final UserService userService;

    public UserManagementMenu(UserService userService) {
        this.userService = userService;
    }

    public void run(User admin) {
        while (true) {
            TerminalUi.banner("ACCOUNT MANAGEMENT", "Review membership and account access");
            System.out.println("  1  View all accounts");
            System.out.println("  2  Review pending accounts");
            System.out.println("  3  Approve an account");
            System.out.println("  4  Deactivate an account");
            System.out.println("  0  Return home");
            int choice = ConsoleInput.readInt("  Select an option: ", 0, 4);
            try {
                switch (choice) {
                    case 1 -> printUsers(userService.listAllUsers(admin));
                    case 2 -> printUsers(userService.listUsersByStatus(admin, AccountStatus.PENDING));
                    case 3 -> {
                        printUsers(userService.listUsersByStatus(admin, AccountStatus.PENDING));
                        int id = ConsoleInput.readInt("User ID to activate (0 to cancel): ", 0, Integer.MAX_VALUE);
                        if (id != 0) {
                            userService.activateAccount(admin, id);
                            System.out.println("Account approved and activated.");
                        }
                    }
                    case 4 -> {
                        printUsers(userService.listUsersByStatus(admin, AccountStatus.ACTIVE));
                        int id = ConsoleInput.readInt("User ID to deactivate (0 to cancel): ", 0, Integer.MAX_VALUE);
                        if (id != 0) {
                            userService.deactivateAccount(admin, id);
                            System.out.println("Account access deactivated.");
                        }
                    }
                    default -> {
                        return;
                    }
                }
            } catch (AccessDeniedException | IllegalArgumentException | IllegalStateException
                     | DataAccessException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private void printUsers(List<User> users) {
        if (users.isEmpty()) {
            System.out.println("(no matching users)");
            return;
        }
        TerminalUi.section("ACCOUNT DIRECTORY  ·  " + users.size() + " record(s)");
        System.out.printf("%-7s %-16s %-24s %-31s %-16s %s%n",
                "ID", "USERNAME", "FULL NAME", "ROLE", "STATUS", "CREATED");
        System.out.println("─".repeat(112));
        for (User u : users) {
            System.out.printf("%-7s %-16s %-24s %-31s %-16s %s%n",
                    "RL-" + String.format("%05d", u.userId()), truncate(u.username(), 16),
                    truncate(u.fullName(), 24), truncate(u.role().getDisplayName(), 31),
                    TerminalUi.status(u.accountStatus().name()), DateTimeUtil.format(u.createdAt()));
        }
    }

    private static String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "...";
    }
}
