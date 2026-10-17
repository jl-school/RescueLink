package com.rescuelink.model;

import com.rescuelink.util.PasswordHasher;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A RescueLink account, modeled as a Java Record (immutable data).
 * The role (Role enum) decides what the user may do; the status decides whether they may log in.
 * toString() deliberately leaves out the password hash so it can never leak into output or logs.
 */
public record User(int userId,
                   String fullName,
                   String username,
                   String passwordHash,
                   Role role,
                   String contactDetails,
                   AccountStatus accountStatus,
                   LocalDateTime createdAt) {

    public User {
        Objects.requireNonNull(fullName, "fullName");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(passwordHash, "passwordHash");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(accountStatus, "accountStatus");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    /** A permission only counts while the account is ACTIVE. */
    public boolean hasPermission(Permission permission) {
        return accountStatus == AccountStatus.ACTIVE && role.grants(permission);
    }

    public boolean verifyPassword(String rawPassword) {
        return PasswordHasher.verify(rawPassword, passwordHash);
    }

    @Override
    public String toString() {
        return "User[userId=" + userId + ", username=" + username + ", role=" + role
                + ", accountStatus=" + accountStatus + "]";
    }
}
