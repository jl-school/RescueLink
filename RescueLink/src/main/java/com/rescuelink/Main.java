package com.rescuelink;

import com.rescuelink.dao.ActivityLogDao;
import com.rescuelink.dao.DataAccessException;
import com.rescuelink.dao.DatabaseInitializer;
import com.rescuelink.dao.UserDao;
import com.rescuelink.ui.Authentication;

import java.util.Scanner;

/**
 * Single entry point. Flow: initialize database -> create DAOs and utilities -> Authentication.start().
 * Menus, login, registration, and role routing all live in ui/Authentication.
 */
public class Main {

    private static final String VERSION = "1.3";

    public static void main(String[] args) {
        // 1. Initialize the database (creates any missing tables) before any menu appears.
        try {
            DatabaseInitializer.initialize();
        } catch (DataAccessException e) {
            System.err.println("Startup failed: " + e.getMessage());
            System.exit(1);
        }

        // 2. Create the shared DAOs and the single Scanner.
        ActivityLogDao activityLogDao = new ActivityLogDao();
        UserDao userDao = new UserDao();
        Scanner scanner = new Scanner(System.in);

        // 3. Hand control to the authentication handler.
        new Authentication(userDao, activityLogDao, scanner, VERSION).start();
        scanner.close();
    }
}
