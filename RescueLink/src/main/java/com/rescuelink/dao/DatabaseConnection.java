package com.rescuelink.dao;

import org.sqlite.SQLiteConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Opens SQLite connections and guarantees that foreign-key constraints are enforced on every one
 * (PRAGMA foreign_keys = ON), and offers small helpers so DAOs never forget to close or roll back.
 * Table creation lives in {@link DatabaseInitializer}.
 */
public final class DatabaseConnection {

    private static final String URL = "jdbc:sqlite:data/rescuelink.db";

    /** A unit of database work that may throw SQLException. */
    @FunctionalInterface
    public interface SqlWork<T> {
        T run(Connection connection) throws SQLException;
    }


    private DatabaseConnection() {
    }

    /**
     * Opens a new connection and immediately runs PRAGMA foreign_keys = ON.
     * SQLite turns foreign keys off by default for every new connection, so this must run each time.
     * Callers must close the connection.
     */
    public static Connection getConnection() throws SQLException {
        SQLiteConfig config = new SQLiteConfig();
        config.setBusyTimeout(5000);
        Connection conn = DriverManager.getConnection(URL, config.toProperties());
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON;");
        } catch (SQLException e) {
            conn.close();
            throw e;
        }
        return conn;
    }

    /** Runs read-only (or single-statement) work on a connection that is always closed afterwards. */
    public static <T> T withConnection(SqlWork<T> work) {
        try (Connection conn = getConnection()) {
            return work.run(conn);
        } catch (SQLException e) {
            throw new DataAccessException("Database error: " + e.getMessage(), e);
        }
    }

    /** Runs work as one transaction: commits if it succeeds, rolls back if anything is thrown. */
    public static <T> T inTransaction(SqlWork<T> work) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                T result = work.run(conn);
                conn.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Database error: " + e.getMessage(), e);
        }
    }
}
