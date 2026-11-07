package com.rescuelink.model;

import java.time.LocalDateTime;

/** A persisted audit event, including the account name used to identify its actor. */
public record ActivityLog(int logId,
                          int userId,
                          String username,
                          String actionType,
                          String affectedTable,
                          Integer affectedRecordId,
                          LocalDateTime timestamp,
                          String changeSummary) {
}
