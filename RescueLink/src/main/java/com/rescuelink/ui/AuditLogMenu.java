package com.rescuelink.ui;

import com.rescuelink.dao.ActivityLogDao;
import com.rescuelink.dao.DataAccessException;
import com.rescuelink.model.ActivityLog;
import com.rescuelink.model.AccessDeniedException;
import com.rescuelink.model.User;
import com.rescuelink.util.DateTimeUtil;

import java.util.List;

/** Administrator-only console view for the system-wide RescueLink audit trail. */
final class AuditLogMenu {

    private final ActivityLogDao activityLogDao;
    private final ConsoleInput input;

    AuditLogMenu(ActivityLogDao activityLogDao, ConsoleInput input) {
        this.activityLogDao = activityLogDao;
        this.input = input;
    }

    void run(User administrator) {
        boolean viewing = true;
        while (viewing) {
            TerminalUi.banner("SYSTEM AUDIT LOGS", "Account activity and RescueLink record changes");
            System.out.println("  1  View all system logs");
            System.out.println("  2  Filter logs by user ID");
            System.out.println("  0  Return to administrator menu");
            int choice = input.readInt("  Select an option: ", 0, 2);
            try {
                switch (choice) {
                    case 1 -> printLogs(activityLogDao.getAllLogs(administrator));
                    case 2 -> {
                        int userId = input.readInt("  User ID: ", 1, Integer.MAX_VALUE);
                        printLogs(activityLogDao.getLogsByUserId(administrator, userId));
                    }
                    default -> viewing = false;
                }
            } catch (DataAccessException e) {
                System.out.println("Could not read the audit logs: " + e.getMessage());
            } catch (AccessDeniedException e) {
                System.out.println("Access denied: " + e.getMessage());
                return;
            }
        }
    }

    private void printLogs(List<ActivityLog> logs) {
        TerminalUi.section("AUDIT LOG ENTRIES (" + logs.size() + ")");
        if (logs.isEmpty()) {
            System.out.println("  No audit log entries were found.");
            return;
        }
        System.out.printf("  %-5s | %-8s | %-18s | %-35s | %-20s%n",
                "LOG ID", "USER ID", "ACTION", "DESCRIPTION", "TIMESTAMP");
        System.out.println("  " + "-".repeat(102));
        for (ActivityLog log : logs) {
            String description = log.username() + ": "
                    + (log.changeSummary() == null ? "" : log.changeSummary());
            if (log.affectedTable() != null) {
                description += " [" + log.affectedTable()
                        + (log.affectedRecordId() == null ? "" : " #" + log.affectedRecordId()) + "]";
            }
            System.out.printf("  %-5d | %-8d | %-18s | %-35s | %-20s%n",
                    log.logId(), log.userId(), fit(log.actionType(), 18), fit(description, 35),
                    DateTimeUtil.format(log.timestamp()));
        }
    }

    private String fit(String value, int width) {
        if (value == null) {
            return "";
        }
        return value.length() <= width ? value : value.substring(0, width - 1) + "…";
    }
}
