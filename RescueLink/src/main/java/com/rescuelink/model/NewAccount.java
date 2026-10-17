package com.rescuelink.model;

/** Simple immutable data carrier for an account that is about to be inserted (a Java record). */
public record NewAccount(String fullName,
                         String username,
                         String passwordHash,
                         Role role,
                         String contactDetails,
                         AccountStatus status) {
}
