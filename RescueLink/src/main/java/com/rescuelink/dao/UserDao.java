package com.rescuelink.dao;

import com.rescuelink.model.AccessDeniedException;
import com.rescuelink.model.AccountStatus;
import com.rescuelink.model.NewAccount;
import com.rescuelink.model.Permission;
import com.rescuelink.model.Role;
import com.rescuelink.model.User;
import com.rescuelink.util.DateTimeUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.Optional;

/**
 * All SQL for the Users table. Every statement uses PreparedStatement.
 * Permission checks re-read the acting user from the database, so a stale session
 * (for example a deactivated account) cannot be used to change data.
 */
public class UserDao {

    private static final String COLUMNS =
            "user_id, full_name, username, password_hash, role, contact_details, account_status, created_at";

    private final ActivityLogDao activityLogDao = new ActivityLogDao();

    // ---------------------------------------------------------------- create

    /** Public self-registration for any role: always PENDING until an administrator approves it. */
    public User register(NewAccount account) {
        if (account.status() != AccountStatus.PENDING) {
            throw new IllegalArgumentException("Self-registration must create a PENDING account.");
        }
        return DatabaseConnection.inTransaction(conn -> insertAndLog(conn, account, "ACCOUNT_REGISTERED"));
    }

    /** First-run setup: only allowed while no ACTIVE administrator exists. */
    public User createFirstAdministrator(NewAccount account) {
        if (account.role() != Role.ADMINISTRATOR || account.status() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("First administrator must be an ACTIVE administrator account.");
        }
        return DatabaseConnection.inTransaction(conn -> {
            if (activeAdministratorExists(conn)) {
                throw new IllegalStateException("An active administrator already exists.");
            }
            return insertAndLog(conn, account, "ADMIN_SETUP");
        });
    }

    // ------------------------------------------------------------------ read

    public Optional<User> findByUsername(String username) {
        return DatabaseConnection.withConnection(conn -> {
            String sql = "SELECT " + COLUMNS + " FROM Users WHERE username = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapRow(rs)) : Optional.<User>empty();
                }
            }
        });
    }

    public boolean usernameExists(String username) {
        return DatabaseConnection.withConnection(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM Users WHERE username = ?")) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    public boolean activeAdministratorExists() {
        return DatabaseConnection.withConnection(this::activeAdministratorExists);
    }

    public List<User> getAllUsers(User actor) {
        return DatabaseConnection.withConnection(conn -> {
            requirePermission(conn, actor, Permission.MANAGE_USERS);
            return queryUsers(conn, "SELECT " + COLUMNS + " FROM Users ORDER BY user_id", null);
        });
    }

    /** Returns every pending registration across all four RescueLink roles. */
    public List<User> getPendingUsers(User actor) {
        return DatabaseConnection.withConnection(conn -> {
            requirePermission(conn, actor, Permission.MANAGE_USERS);
            return queryUsers(conn,
                    "SELECT " + COLUMNS + " FROM Users WHERE account_status = ? ORDER BY user_id",
                    AccountStatus.PENDING.name());
        });
    }

    public List<User> findByStatus(User actor, AccountStatus status) {
        return DatabaseConnection.withConnection(conn -> {
            requirePermission(conn, actor, Permission.MANAGE_USERS);
            return queryUsers(conn,
                    "SELECT " + COLUMNS + " FROM Users WHERE account_status = ? ORDER BY user_id", status.name());
        });
    }

    // ---------------------------------------------------------------- update

    /** Activates or deactivates an account (soft delete). Logged in ActivityLogs. */
    public void updateAccountStatus(User actor, int targetUserId, AccountStatus newStatus) {
        if (newStatus == AccountStatus.PENDING) {
            throw new IllegalArgumentException("An account cannot be set back to PENDING.");
        }
        DatabaseConnection.inTransaction(conn -> {
            User admin = requirePermission(conn, actor, Permission.MANAGE_USERS);
            User target = findById(conn, targetUserId).orElseThrow(
                    () -> new IllegalArgumentException("No user with ID " + targetUserId + " exists."));
            if (target.userId() == admin.userId() && newStatus != AccountStatus.ACTIVE) {
                throw new IllegalStateException("You cannot deactivate your own account.");
            }
            if (target.accountStatus() == newStatus) {
                throw new IllegalStateException("That account is already " + newStatus + ".");
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE Users SET account_status = ? WHERE user_id = ?")) {
                ps.setString(1, newStatus.name());
                ps.setInt(2, targetUserId);
                ps.executeUpdate();
            }
            String action = newStatus == AccountStatus.ACTIVE ? "ACCOUNT_ACTIVATED" : "ACCOUNT_DEACTIVATED";
            activityLogDao.record(conn, admin.userId(), action, "Users", targetUserId,
                    "Status of '" + target.username() + "' changed from "
                            + target.accountStatus() + " to " + newStatus);
            return null;
        });
    }

    /** Updates an account's RescueLink role and status together, with permission and audit checks. */
    public void updateUserRoleAndStatus(User actor, int targetUserId, String newRole, String newStatus) {
        Role role = parseRole(newRole);
        AccountStatus status = parseStatus(newStatus);
        DatabaseConnection.inTransaction(conn -> {
            User admin = requirePermission(conn, actor, Permission.MANAGE_USERS);
            User target = findById(conn, targetUserId).orElseThrow(
                    () -> new IllegalArgumentException("No user with ID " + targetUserId + " exists."));
            if (target.userId() == admin.userId()) {
                throw new IllegalStateException("Modifying the currently active admin account is restricted.");
            }
            if (target.role() == role && target.accountStatus() == status) {
                throw new IllegalStateException("The account already has that role and status.");
            }

            String sql = "UPDATE Users SET role = ?, account_status = ? WHERE user_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, role.name());
                ps.setString(2, status.name());
                ps.setInt(3, targetUserId);
                if (ps.executeUpdate() != 1) {
                    throw new IllegalStateException("The account could not be updated.");
                }
            }
            activityLogDao.record(conn, admin.userId(), "ACCOUNT_ROLE_STATUS_UPDATED", "Users", targetUserId,
                    "Account '" + target.username() + "' changed from " + target.role() + "/"
                            + target.accountStatus() + " to " + role + "/" + status);
            return null;
        });
    }

    /** Approves a pending registration by activating it, preserving the ERD's account_status values. */
    public void approveUser(User actor, int targetUserId) {
        DatabaseConnection.inTransaction(conn -> {
            User admin = requirePermission(conn, actor, Permission.MANAGE_USERS);
            User target = findById(conn, targetUserId).orElseThrow(
                    () -> new IllegalArgumentException("No user with ID " + targetUserId + " exists."));
            if (target.accountStatus() != AccountStatus.PENDING) {
                throw new IllegalStateException("Only PENDING accounts can be approved.");
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE Users SET account_status = ? WHERE user_id = ? AND account_status = ?")) {
                ps.setString(1, AccountStatus.ACTIVE.name());
                ps.setInt(2, targetUserId);
                ps.setString(3, AccountStatus.PENDING.name());
                if (ps.executeUpdate() != 1) {
                    throw new IllegalStateException("The account is no longer pending approval.");
                }
            }
            activityLogDao.record(conn, admin.userId(), "ACCOUNT_ACTIVATED", "Users", targetUserId,
                    "Account '" + target.username() + "' approved; status changed from PENDING to ACTIVE");
            return null;
        });
    }

    // --------------------------------------------------------------- helpers

    private User insertAndLog(Connection conn, NewAccount a, String action) throws SQLException {
        String createdAt = DateTimeUtil.nowString();
        String sql = "INSERT INTO Users (full_name, username, password_hash, role, contact_details, "
                + "account_status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        int newId;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, a.fullName());
            ps.setString(2, a.username());
            ps.setString(3, a.passwordHash());
            ps.setString(4, a.role().name());
            ps.setString(5, a.contactDetails());
            ps.setString(6, a.status().name());
            ps.setString(7, createdAt);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("The new user ID could not be read.");
                }
                newId = keys.getInt(1);
            }
        }
        activityLogDao.record(conn, newId, action, "Users", newId,
                "Account '" + a.username() + "' created as " + a.role() + " with status " + a.status());
        return new User(newId, a.fullName(), a.username(), a.passwordHash(), a.role(),
                a.contactDetails(), a.status(), DateTimeUtil.parse(createdAt));
    }

    private Optional<User> findById(Connection conn, int userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT " + COLUMNS + " FROM Users WHERE user_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.<User>empty();
            }
        }
    }

    private List<User> queryUsers(Connection conn, String sql, String param) throws SQLException {
        List<User> users = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) {
                ps.setString(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(mapRow(rs));
                }
            }
        }
        return users;
    }

    private boolean activeAdministratorExists(Connection conn) throws SQLException {
        String sql = "SELECT 1 FROM Users WHERE role = ? AND account_status = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, Role.ADMINISTRATOR.name());
            ps.setString(2, AccountStatus.ACTIVE.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static Role parseRole(String value) {
        try {
            return Role.valueOf((value == null ? "" : value.trim()).toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid role. Choose one of the RescueLink roles.");
        }
    }

    private static AccountStatus parseStatus(String value) {
        String normalized = (value == null ? "" : value.trim()).toUpperCase(Locale.ROOT);
        if (normalized.equals("APPROVED")) {
            normalized = AccountStatus.ACTIVE.name();
        }
        try {
            return AccountStatus.valueOf(normalized);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid status. Choose PENDING, APPROVED/ACTIVE, or DEACTIVATED.");
        }
    }

    /** Re-reads the actor from the database and requires an existing, ACTIVE account with the permission. */
    private User requirePermission(Connection conn, User actor, Permission permission) throws SQLException {
        if (actor == null) {
            throw new AccessDeniedException("You are not logged in.");
        }
        User fresh = findById(conn, actor.userId()).orElseThrow(
                () -> new AccessDeniedException("Your account no longer exists."));
        if (fresh.accountStatus() != AccountStatus.ACTIVE) {
            throw new AccessDeniedException("Your account is not active.");
        }
        if (!fresh.hasPermission(permission)) {
            throw new AccessDeniedException("You do not have permission to perform this action.");
        }
        return fresh;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("user_id"),
                rs.getString("full_name"),
                rs.getString("username"),
                rs.getString("password_hash"),
                Role.valueOf(rs.getString("role")),
                rs.getString("contact_details"),
                AccountStatus.valueOf(rs.getString("account_status")),
                DateTimeUtil.parse(rs.getString("created_at")));
    }
}
