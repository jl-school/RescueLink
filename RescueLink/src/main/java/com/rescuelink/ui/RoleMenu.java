package com.rescuelink.ui;

import com.rescuelink.dao.DataAccessException;
import com.rescuelink.model.User;
import com.rescuelink.service.UserService;
import com.rescuelink.util.DateTimeUtil;

import java.util.List;

/**
 * Base class for the role-based home menus (template method pattern).
 * It draws the shared parts (header, profile, sign out); each subclass adds only the options
 * that belong to its role. Logging out returns to the caller (Authentication), not exit.
 */
public abstract class RoleMenu {

    protected final UserService userService;
    protected final ConsoleInput input;

    protected RoleMenu(UserService userService, ConsoleInput input) {
        this.userService = userService;
        this.input = input;
    }

    /** Options specific to this role, shown as options 2, 3, ... */
    protected abstract List<String> roleOptions(User user);

    /** Runs the role option at the given zero-based position in {@link #roleOptions}. */
    protected abstract void handleRoleOption(User user, int index);

    public final void run(User user) {
        while (true) {
            TerminalUi.banner("YOUR RESCUELINK", user.fullName() + "  |  " + user.role().getDisplayName());
            System.out.println("  " + user.role().getDescription());
            System.out.println("  Role modules for households, incidents and relief are coming soon.");
            System.out.println();
            System.out.println("  1  View my profile");
            List<String> options = roleOptions(user);
            for (int i = 0; i < options.size(); i++) {
                System.out.println("  " + (i + 2) + "  " + options.get(i));
            }
            System.out.println("  0  Sign out");
            int choice = input.readInt("  Select an option: ", 0, options.size() + 1);
            try {
                if (choice == 0) {
                    userService.logout(user);
                    System.out.println("You have been logged out.");
                    return;
                } else if (choice == 1) {
                    showProfile(user);
                } else {
                    handleRoleOption(user, choice - 2);
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
