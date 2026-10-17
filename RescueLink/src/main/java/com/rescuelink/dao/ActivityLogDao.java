package com.rescuelink.dao;

import com.rescuelink.util.DateTimeUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

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
