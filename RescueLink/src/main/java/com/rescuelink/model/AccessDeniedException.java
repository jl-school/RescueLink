package com.rescuelink.model;

/** Thrown when a user attempts an action their role or account status does not allow. */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
        super(message);
    }
}
