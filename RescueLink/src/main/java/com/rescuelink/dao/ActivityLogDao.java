package com.rescuelink.dao;

import com.rescuelink.util.DateTimeUtil;
import com.rescuelink.model.ActivityLog;
import com.rescuelink.model.Permission;
import com.rescuelink.model.Role;
import com.rescuelink.model.AccountStatus;
import com.rescuelink.model.AccessDeniedException;
import com.rescuelink.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/** Writes entries to the ActivityLogs table (audit trail). */
public class ActivityLogDao {

    private static final String INSERT_SQL =
            "INSERT INTO ActivityLogs (user_id, action_type, affected_table, affected_record_id, timestamp, change_summary) "
                    + "VALUES (?, ?, ?, ?, ?, ?)";

    /** Records an entry on its own connection. */
    public void record(int userId, String actionType, String affectedTable, Integer affectedRecordId, String summary) {
        DatabaseConnection.withConnection(conn -> {
            record(conn, userId, actionType, affectedTable, affectedRecordId, summary);
            return null;
        });
    }

    /** Records a RescueLink business action without a linked record, in its own transaction. */
    public void logAction(int userId, String actionType, String description) {
        record(userId, actionType, null, null, description);
    }

    /** Returns all audit events, newest first, with the actor's current username. */
    public List<ActivityLog> getAllLogs(User actor) {
        return DatabaseConnection.withConnection(conn -> {
            requireAuditPermission(conn, actor);
            return queryLogs(conn,
                "SELECT l.log_id, l.user_id, u.username, l.action_type, l.affected_table, "
                        + "l.affected_record_id, l.timestamp, l.change_summary "
                        + "FROM ActivityLogs l JOIN Users u ON u.user_id = l.user_id "
                        + "ORDER BY l.timestamp DESC, l.log_id DESC", null);
        });
    }

    /** Returns audit events for one account, newest first. */
    public List<ActivityLog> getLogsByUserId(User actor, int userId) {
        return DatabaseConnection.withConnection(conn -> {
            requireAuditPermission(conn, actor);
            return queryLogs(conn,
                "SELECT l.log_id, l.user_id, u.username, l.action_type, l.affected_table, "
                        + "l.affected_record_id, l.timestamp, l.change_summary "
                        + "FROM ActivityLogs l JOIN Users u ON u.user_id = l.user_id "
                        + "WHERE l.user_id = ? ORDER BY l.timestamp DESC, l.log_id DESC", userId);
        });
    }

    private void requireAuditPermission(Connection conn, User actor) throws SQLException {
        if (actor == null) {
            throw new AccessDeniedException("You are not logged in.");
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT role, account_status FROM Users WHERE user_id = ?")) {
            ps.setInt(1, actor.userId());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new AccessDeniedException("Your account no longer exists.");
                }
                Role role = Role.valueOf(rs.getString("role"));
                AccountStatus status = AccountStatus.valueOf(rs.getString("account_status"));
                if (status != AccountStatus.ACTIVE || !role.grants(Permission.VIEW_ACTIVITY_LOGS)) {
                    throw new AccessDeniedException("You do not have permission to view system audit logs.");
                }
            }
        }
    }

    private List<ActivityLog> queryLogs(Connection conn, String sql, Integer userId) throws SQLException {
        List<ActivityLog> logs = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (userId != null) {
                ps.setInt(1, userId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int recordId = rs.getInt("affected_record_id");
                    Integer affectedRecordId = rs.wasNull() ? null : recordId;
                    logs.add(new ActivityLog(rs.getInt("log_id"), rs.getInt("user_id"),
                            rs.getString("username"), rs.getString("action_type"),
                            rs.getString("affected_table"), affectedRecordId,
                            DateTimeUtil.parse(rs.getString("timestamp")),
                            rs.getString("change_summary")));
                }
            }
        }
        return logs;
    }

    /** Records an entry on an existing connection so it commits or rolls back with the main change. */
    public void record(Connection conn, int userId, String actionType, String affectedTable,
                       Integer affectedRecordId, String summary) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            ps.setInt(1, userId);
            ps.setString(2, actionType);
            ps.setString(3, affectedTable);
            if (affectedRecordId == null) {
                ps.setNull(4, Types.INTEGER);
            } else {
                ps.setInt(4, affectedRecordId);
            }
            ps.setString(5, DateTimeUtil.nowString());
            ps.setString(6, summary);
            ps.executeUpdate();
        }
    }
}
