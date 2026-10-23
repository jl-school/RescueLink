package com.rescuelink.ui;

import com.rescuelink.dao.DataAccessException;
import com.rescuelink.model.AccessDeniedException;
import com.rescuelink.model.AccountStatus;
import com.rescuelink.model.User;
import com.rescuelink.service.UserService;

import java.util.List;

/** Administrator-only screens for reviewing registrations and managing account access. */
public class UserManagementMenu {

    private static final String TABLE_BORDER = "+-------+-----------------+--------------+------------+";

    private final UserService userService;
    private final ConsoleInput input;

    public UserManagementMenu(UserService userService, ConsoleInput input) {
        this.userService = userService;
        this.input = input;
    }

    public void run(User admin) {
        while (true) {
            TerminalUi.banner("ACCOUNT MANAGEMENT", "Review membership and account access");
            System.out.println("  1  View all system users");
            System.out.println("  2  View pending registrations");
            System.out.println("  3  Approve a pending registration");
            System.out.println("  4  Deactivate an active account");
            System.out.println("  0  Return home");
            int choice = input.readInt("  Select an option: ", 0, 4);
            try {
                switch (choice) {
                    case 1 -> printUsers("ALL SYSTEM USERS", userService.getAllUsers(admin));
                    case 2 -> printUsers("PENDING REGISTRATIONS", userService.getPendingUsers(admin));
                    case 3 -> approvePendingUser(admin);
                    case 4 -> deactivateActiveUser(admin);
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

    private void approvePendingUser(User admin) {
        List<User> pendingUsers = userService.getPendingUsers(admin);
        printUsers("PENDING REGISTRATIONS", pendingUsers);
        if (pendingUsers.isEmpty()) {
            return;
        }

        int userId = input.readInt("Target User ID (0 to cancel): ", 0, Integer.MAX_VALUE);
        if (userId == 0) {
            System.out.println("Approval cancelled. No account was changed.");
            return;
        }
        if (!containsUser(pendingUsers, userId)) {
            System.out.println("That ID is not in the pending list. No account was changed.");
            return;
        }

        userService.approveUser(admin, userId);
        System.out.println("Registration approved. Account status is ACTIVE.");
    }

    private void deactivateActiveUser(User admin) {
        List<User> activeUsers = userService.listUsersByStatus(admin, AccountStatus.ACTIVE);
        printUsers("ACTIVE ACCOUNTS", activeUsers);
        if (activeUsers.isEmpty()) {
            return;
        }

        int userId = input.readInt("Target User ID (0 to cancel): ", 0, Integer.MAX_VALUE);
        if (userId == 0) {
            System.out.println("Deactivation cancelled. No account was changed.");
            return;
        }
        if (!containsUser(activeUsers, userId)) {
            System.out.println("That ID is not in the active account list. No account was changed.");
            return;
        }

        userService.deactivateAccount(admin, userId);
        System.out.println("Account access deactivated.");
    }

    private void printUsers(String title, List<User> users) {
        TerminalUi.section(title + " (" + users.size() + " record(s))");
        System.out.println(TABLE_BORDER);
        System.out.printf("| %-5s | %-15s | %-12s | %-10s |%n",
                "ID", "USERNAME", "ROLE", "STATUS");
        System.out.println(TABLE_BORDER);

        if (users.isEmpty()) {
            System.out.printf("| %-5s | %-15s | %-12s | %-10s |%n",
                    "-", "No records", "", "");
        } else {
            for (User user : users) {
                System.out.printf("| %-5s | %-15s | %-12s | %-10s |%n",
                        user.userId(), truncate(user.username(), 15), roleCode(user), statusCode(user));
            }
        }
        System.out.println(TABLE_BORDER);
        System.out.println("Role codes: ADMIN = System Administrator / Barangay Official, "
                + "RESPONDER = Disaster Response Personnel, RELIEF = Relief Coordinator, "
                + "RESIDENT = Resident / Requester.");
        System.out.println("Status code: DEACTIV. = DEACTIVATED.");
    }

    private static boolean containsUser(List<User> users, int userId) {
        return users.stream().anyMatch(user -> user.userId() == userId);
    }

    private static String roleCode(User user) {
        return switch (user.role()) {
            case ADMINISTRATOR -> "ADMIN";
            case RESPONSE_PERSONNEL -> "RESPONDER";
            case RELIEF_COORDINATOR -> "RELIEF";
            case RESIDENT -> "RESIDENT";
        };
    }

    private static String statusCode(User user) {
        return switch (user.accountStatus()) {
            case ACTIVE -> "ACTIVE";
            case PENDING -> "PENDING";
            case DEACTIVATED -> "DEACTIV.";
        };
    }

    private static String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }
}
