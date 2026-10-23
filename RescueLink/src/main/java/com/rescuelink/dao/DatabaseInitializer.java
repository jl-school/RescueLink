package com.rescuelink.dao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Makes sure the data folder and all nine RescueLink tables exist before the CLI menu starts.
 * Every table uses CREATE TABLE IF NOT EXISTS, so existing data is never deleted and
 * it is safe to call on every start.
 */
public final class DatabaseInitializer {

    private static final Path DATA_DIR = Path.of("data");

    private static final String[] SCHEMA = {
            """
            CREATE TABLE IF NOT EXISTS Users (
                user_id         INTEGER PRIMARY KEY AUTOINCREMENT,
                full_name       TEXT NOT NULL,
                username        TEXT NOT NULL UNIQUE COLLATE NOCASE,
                password_hash   TEXT NOT NULL,
                role            TEXT NOT NULL CHECK (role IN
                                ('ADMINISTRATOR','RESPONSE_PERSONNEL','RELIEF_COORDINATOR','RESIDENT')),
                contact_details TEXT,
                account_status  TEXT NOT NULL DEFAULT 'PENDING'
                                CHECK (account_status IN ('PENDING','ACTIVE','DEACTIVATED')),
                created_at      TEXT NOT NULL DEFAULT (datetime('now','localtime'))
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS Households (
                household_id        INTEGER PRIMARY KEY AUTOINCREMENT,
                head_name           TEXT NOT NULL,
                address_purok       TEXT NOT NULL,
                contact_no          TEXT,
                member_count        INTEGER NOT NULL CHECK (member_count >= 1),
                num_seniors         INTEGER NOT NULL DEFAULT 0 CHECK (num_seniors >= 0),
                num_pwd             INTEGER NOT NULL DEFAULT 0 CHECK (num_pwd >= 0),
                num_children        INTEGER NOT NULL DEFAULT 0 CHECK (num_children >= 0),
                num_pregnant        INTEGER NOT NULL DEFAULT 0 CHECK (num_pregnant >= 0),
                vulnerability_level TEXT NOT NULL DEFAULT 'LOW'
                                    CHECK (vulnerability_level IN ('LOW','MODERATE','HIGH')),
                registered_by       INTEGER NOT NULL REFERENCES Users(user_id),
                CHECK (num_seniors <= member_count AND num_pwd <= member_count
                       AND num_children <= member_count AND num_pregnant <= member_count)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS Incidents (
                incident_id        INTEGER PRIMARY KEY AUTOINCREMENT,
                reported_by        INTEGER NOT NULL REFERENCES Users(user_id),
                incident_type      TEXT NOT NULL,
                location           TEXT NOT NULL,
                description        TEXT,
                reported_at        TEXT NOT NULL DEFAULT (datetime('now','localtime')),
                status             TEXT NOT NULL DEFAULT 'OPEN'
                                   CHECK (status IN ('OPEN','IN_PROGRESS','RESOLVED','ARCHIVED','VOIDED')),
                assigned_responder INTEGER REFERENCES Users(user_id),
                response_notes     TEXT
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS AssistanceRequests (
                request_id   INTEGER PRIMARY KEY AUTOINCREMENT,
                household_id INTEGER NOT NULL REFERENCES Households(household_id),
                submitted_by INTEGER NOT NULL REFERENCES Users(user_id),
                incident_id  INTEGER REFERENCES Incidents(incident_id),
                request_type TEXT NOT NULL,
                priority     TEXT NOT NULL DEFAULT 'MEDIUM'
                             CHECK (priority IN ('LOW','MEDIUM','HIGH','CRITICAL')),
                details      TEXT,
                submitted_at TEXT NOT NULL DEFAULT (datetime('now','localtime')),
                status       TEXT NOT NULL DEFAULT 'OPEN'
                             CHECK (status IN ('OPEN','IN_PROGRESS','FULFILLED','ARCHIVED','VOIDED')),
                assigned_to  INTEGER REFERENCES Users(user_id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS EvacuationCenters (
                center_id         INTEGER PRIMARY KEY AUTOINCREMENT,
                center_name       TEXT NOT NULL UNIQUE,
                location          TEXT NOT NULL,
                capacity          INTEGER NOT NULL CHECK (capacity > 0),
                current_occupancy INTEGER NOT NULL DEFAULT 0 CHECK (current_occupancy >= 0),
                center_status     TEXT NOT NULL DEFAULT 'OPEN'
                                  CHECK (center_status IN ('OPEN','FULL','CLOSED')),
                contact_person    TEXT,
                last_updated      TEXT NOT NULL DEFAULT (datetime('now','localtime')),
                CHECK (current_occupancy <= capacity)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS ReliefInventory (
                item_id          INTEGER PRIMARY KEY AUTOINCREMENT,
                item_name        TEXT NOT NULL UNIQUE COLLATE NOCASE,
                category         TEXT NOT NULL,
                current_quantity INTEGER NOT NULL DEFAULT 0 CHECK (current_quantity >= 0),
                unit             TEXT NOT NULL,
                reorder_level    INTEGER NOT NULL DEFAULT 0 CHECK (reorder_level >= 0),
                last_updated     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS ReliefTransactions (
                transaction_id      INTEGER PRIMARY KEY AUTOINCREMENT,
                item_id             INTEGER NOT NULL REFERENCES ReliefInventory(item_id),
                transaction_type    TEXT NOT NULL CHECK (transaction_type IN ('RECEIVED','DISTRIBUTED')),
                quantity            INTEGER NOT NULL CHECK (quantity > 0),
                household_id        INTEGER REFERENCES Households(household_id),
                center_id           INTEGER REFERENCES EvacuationCenters(center_id),
                related_incident_id INTEGER REFERENCES Incidents(incident_id),
                recorded_by         INTEGER NOT NULL REFERENCES Users(user_id),
                transaction_date    TEXT NOT NULL DEFAULT (datetime('now','localtime')),
                notes               TEXT,
                CHECK (transaction_type = 'RECEIVED' OR household_id IS NOT NULL OR center_id IS NOT NULL)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS ActivityLogs (
                log_id             INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id            INTEGER NOT NULL REFERENCES Users(user_id),
                action_type        TEXT NOT NULL,
                affected_table     TEXT,
                affected_record_id INTEGER,
                timestamp          TEXT NOT NULL DEFAULT (datetime('now','localtime')),
                change_summary     TEXT
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS EvacueeRecords (
                record_id    INTEGER PRIMARY KEY AUTOINCREMENT,
                household_id INTEGER NOT NULL REFERENCES Households(household_id),
                center_id    INTEGER NOT NULL REFERENCES EvacuationCenters(center_id),
                checkin_at   TEXT NOT NULL DEFAULT (datetime('now','localtime')),
                checkout_at  TEXT,
                headcount    INTEGER NOT NULL CHECK (headcount > 0),
                CHECK (checkout_at IS NULL OR checkout_at >= checkin_at)
            )
            """
    };

    private DatabaseInitializer() {
    }

    /** Creates the data folder and any missing tables. Throws DataAccessException on failure. */
    public static void initialize() {
        try {
            Files.createDirectories(DATA_DIR);
        } catch (IOException e) {
            throw new DataAccessException("Could not create the data folder: " + e.getMessage(), e);
        }
        // getConnection() already turns PRAGMA foreign_keys = ON, so the FKs below are enforced.
        try (Connection conn = DatabaseConnection.getConnection(); Statement st = conn.createStatement()) {
            for (String sql : SCHEMA) {
                st.execute(sql);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not initialize the database: " + e.getMessage(), e);
        }
    }
}
