package com.rescuelink;

import com.rescuelink.dao.ActivityLogDao;
import com.rescuelink.dao.DataAccessException;
import com.rescuelink.dao.DatabaseConnection;
import com.rescuelink.dao.UserDao;
import com.rescuelink.service.UserService;
import com.rescuelink.ui.AuthenticationMenu;
import com.rescuelink.ui.MainMenu;
import com.rescuelink.ui.UserManagementMenu;
import com.rescuelink.ui.UserMenu;

/** Application entry point: prepares the database, wires the layers together, and starts the menu. */
public class Main {

    public static void main(String[] args) {
        try {
            DatabaseConnection.initializeDatabase();
        } catch (DataAccessException e) {
            System.err.println("Startup failed: " + e.getMessage());
            System.exit(1);
        }

        ActivityLogDao activityLogDao = new ActivityLogDao();
        UserDao userDao = new UserDao();
        UserService userService = new UserService(userDao, activityLogDao);

        UserManagementMenu userManagementMenu = new UserManagementMenu(userService);
        UserMenu userMenu = new UserMenu(userService, userManagementMenu);
        AuthenticationMenu authenticationMenu = new AuthenticationMenu(userService);

        new MainMenu(userService, authenticationMenu, userMenu).run();
    }
}
