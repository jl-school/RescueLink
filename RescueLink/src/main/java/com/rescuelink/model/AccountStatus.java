package com.rescuelink.model;

/** Lifecycle of a user account. New accounts start as PENDING until an administrator activates them. */
public enum AccountStatus {
    PENDING,
    ACTIVE,
    DEACTIVATED
}
