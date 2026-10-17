package com.rescuelink.model;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * The four RescueLink user roles from the system proposal.
 * Each role knows its display name, a short description, and the permissions it grants.
 */
public enum Role {
    ADMINISTRATOR("System Administrator / Barangay Official",
            "Manages accounts and permissions, maintains household records, and reviews the dashboard, reports, and activity logs.",
            Permission.MANAGE_USERS, Permission.MANAGE_HOUSEHOLDS, Permission.VIEW_HOUSEHOLDS,
            Permission.MANAGE_INCIDENTS, Permission.MANAGE_REQUESTS, Permission.SUBMIT_REQUEST,
            Permission.MANAGE_EVACUATION, Permission.VIEW_EVACUATION,
            Permission.MANAGE_INVENTORY, Permission.VIEW_INVENTORY,
            Permission.VIEW_DASHBOARD, Permission.VIEW_REPORTS, Permission.VIEW_ACTIVITY_LOGS),

    RESPONSE_PERSONNEL("Disaster Response Personnel",
            "Creates and updates incident reports, records rescue requests, and updates response status.",
            Permission.MANAGE_INCIDENTS, Permission.MANAGE_REQUESTS, Permission.SUBMIT_REQUEST,
            Permission.VIEW_HOUSEHOLDS, Permission.VIEW_EVACUATION),

    RELIEF_COORDINATOR("Relief Coordinator",
            "Records donations and relief stock, records distributions, and reviews inventory and low-stock alerts.",
            Permission.MANAGE_INVENTORY, Permission.VIEW_INVENTORY, Permission.VIEW_EVACUATION),

    RESIDENT("Resident / Requester",
            "Submits assistance requests and views the status of your own requests.",
            Permission.SUBMIT_REQUEST, Permission.VIEW_OWN_REQUESTS);

    private final String displayName;
    private final String description;
    private final Set<Permission> permissions;

    Role(String displayName, String description, Permission... permissions) {
        this.displayName = displayName;
        this.description = description;
        this.permissions = EnumSet.copyOf(Arrays.asList(permissions));
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean grants(Permission permission) {
        return permissions.contains(permission);
    }
}
