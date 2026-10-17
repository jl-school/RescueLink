package com.rescuelink.service;

/** A problem with user input or credentials. The message is safe to show to the user. */
public class AuthException extends Exception {

    public AuthException(String message) {
        super(message);
    }
}
