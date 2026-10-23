package com.rescuelink.ui;

import com.rescuelink.model.User;
import com.rescuelink.service.UserService;

import java.util.List;

/** Home menu for Resident / Requester. Role-specific modules will be added in later versions. */
public class ResidentMenu extends RoleMenu {

    public ResidentMenu(UserService userService, ConsoleInput input) {
        super(userService, input);
    }

    @Override
    protected List<String> roleOptions(User user) {
        return List.of();
    }

    @Override
    protected void handleRoleOption(User user, int index) {
        // No role-specific options yet.
    }
}
