package com.rescuelink.ui;

import com.rescuelink.model.User;
import com.rescuelink.service.UserService;

import java.util.List;

/** Home menu for System Administrator / Barangay Official. */
public class AdministratorMenu extends RoleMenu {

    private final UserManagementMenu userManagementMenu;

    public AdministratorMenu(UserService userService, ConsoleInput input) {
        super(userService, input);
        this.userManagementMenu = new UserManagementMenu(userService, input);
    }

    @Override
    protected List<String> roleOptions(User user) {
        return List.of("Manage accounts");
    }

    @Override
    protected void handleRoleOption(User user, int index) {
        if (index == 0) {
            userManagementMenu.run(user);
        }
    }
}
