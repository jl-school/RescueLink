package com.rescuelink.ui;

import com.rescuelink.dao.DataAccessException;
import com.rescuelink.model.AccessDeniedException;
import com.rescuelink.model.AccountStatus;
import com.rescuelink.model.Role;
import com.rescuelink.model.User;
import com.rescuelink.service.UserService;

import java.util.List;
import java.util.Locale;

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
            System.out.println("  5  Update a user's role and status");
            System.out.println("  0  Return home");
            int choice = input.readInt("  Select an option: ", 0, 5);
            try {
                switch (choice) {
                    case 1 -> printUsers("ALL SYSTEM USERS", userService.getAllUsers(admin));
                    case 2 -> printUsers("PENDING REGISTRATIONS", userService.getPendingUsers(admin));
                    case 3 -> approvePendingUser(admin);
                    case 4 -> deactivateActiveUser(admin);
                    case 5 -> updateUserRoleAndStatus(admin);
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

    private void updateUserRoleAndStatus(User admin) {
        List<User> users = userService.getAllUsers(admin);
        printUsers("ALL SYSTEM USERS", users);
        if (users.isEmpty()) {
            return;
        }

        int userId = readIntInput("Target User ID (0 to cancel): ");
        if (userId == 0) {
            System.out.println("Update cancelled. No account was changed.");
            return;
        }
        User target = users.stream().filter(user -> user.userId() == userId).findFirst().orElse(null);
        if (target == null) {
            System.out.println("That ID is not in the user list. No account was changed.");
            return;
        }
        if (target.userId() == admin.userId()) {
            System.out.println("WARNING: Modifying the currently active admin account is restricted.");
            return;
        }

        Role role = readValidRole();
        if (role == null) {
            System.out.println("Update cancelled. No account was changed.");
            return;
        }
        String status = readValidStatus();
        if (status == null) {
            System.out.println("Update cancelled. No account was changed.");
            return;
        }

        userService.updateUserRoleAndStatus(admin, userId, role.name(), status);
        System.out.println("Updated " + target.username() + ": role " + roleCode(role) + ", status " + status + ".");
    }

    /** ConsoleInput repeats after letters, symbols, and out-of-range values. */
    private int readIntInput(String prompt) {
        return input.readInt(prompt, 0, Integer.MAX_VALUE);
    }

    /** Accepts the canonical proposal role names, menu numbers, and the short codes used in the user table. */
    private Role readValidRole() {
        Role[] roles = Role.values();
        System.out.println("New role (enter its name/code or 0 to cancel):");
        for (int i = 0; i < roles.length; i++) {
            System.out.printf("  %d  %-20s %s%n", i + 1, roles[i].name(), roles[i].getDisplayName());
        }
        while (true) {
            String value = input.readLine("  Role: ").trim().toUpperCase(Locale.ROOT);
            if (value.equals("0")) {
                return null;
            }
            try {
                int choice = Integer.parseInt(value);
                if (choice >= 1 && choice <= roles.length) {
                    return roles[choice - 1];
                }
            } catch (NumberFormatException ignored) {
                // Try the role name or table code below.
            }
            String canonical = switch (value) {
                case "ADMIN" -> "ADMINISTRATOR";
                case "RESPONDER" -> "RESPONSE_PERSONNEL";
                case "RELIEF" -> "RELIEF_COORDINATOR";
                default -> value;
            };
            try {
                return Role.valueOf(canonical);
            } catch (IllegalArgumentException ignored) {
                System.out.println("Please enter ADMINISTRATOR, RESPONSE_PERSONNEL, RELIEF_COORDINATOR, RESIDENT, or 0.");
            }
        }
    }

    /** APPROVED is the proposal's label for ACTIVE in the existing account-status model. */
    private String readValidStatus() {
        System.out.println("Valid status: PENDING, APPROVED (ACTIVE), or DEACTIVATED. Enter 0 to cancel.");
        while (true) {
            String value = input.readLine("  Status: ").trim().toUpperCase(Locale.ROOT);
            if (value.equals("0")) {
                return null;
            }
            if (value.equals("PENDING") || value.equals("DEACTIVATED")) {
                return value;
            }
            if (value.equals("APPROVED") || value.equals("ACTIVE")) {
                return "ACTIVE";
            }
            System.out.println("Please enter PENDING, APPROVED/ACTIVE, DEACTIVATED, or 0.");
        }
    }

    private static String roleCode(Role role) {
        return switch (role) {
            case ADMINISTRATOR -> "ADMIN";
            case RESPONSE_PERSONNEL -> "RESPONDER";
            case RELIEF_COORDINATOR -> "RELIEF";
            case RESIDENT -> "RESIDENT";
        };
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
