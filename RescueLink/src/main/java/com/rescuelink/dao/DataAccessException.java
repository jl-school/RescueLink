package com.rescuelink.dao;

/** Unchecked wrapper for SQLException so upper layers can show a clean error message. */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
