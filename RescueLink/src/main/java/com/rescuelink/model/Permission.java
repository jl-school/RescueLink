package com.rescuelink.model;

/**
 * Fine-grained permissions granted to roles. Each User subclass decides which ones it holds.
 * Data-access and service code checks these, not just the menus.
 */
public enum Permission {
    MANAGE_USERS,
    MANAGE_HOUSEHOLDS,
    VIEW_HOUSEHOLDS,
    MANAGE_INCIDENTS,
    MANAGE_REQUESTS,
    SUBMIT_REQUEST,
    VIEW_OWN_REQUESTS,
    MANAGE_EVACUATION,
    VIEW_EVACUATION,
    MANAGE_INVENTORY,
    VIEW_INVENTORY,
    VIEW_DASHBOARD,
    VIEW_REPORTS,
    VIEW_ACTIVITY_LOGS
}
