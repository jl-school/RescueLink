package com.rescuelink.service;

import com.rescuelink.dao.ActivityLogDao;
import com.rescuelink.dao.UserDao;
import com.rescuelink.model.AccountStatus;
import com.rescuelink.model.NewAccount;
import com.rescuelink.model.Permission;
import com.rescuelink.model.Role;
import com.rescuelink.model.User;
import com.rescuelink.util.PasswordHasher;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Business rules for registration, login, logout, and account approval.
 * The UI talks to this class; this class talks to the DAOs.
 */
public class UserService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{4,20}$");

    private final UserDao userDao;
    private final ActivityLogDao activityLogDao;

    public UserService(UserDao userDao, ActivityLogDao activityLogDao) {
        this.userDao = userDao;
        this.activityLogDao = activityLogDao;
    }

    // ------------------------------------------------- setup and registration

    public boolean needsFirstAdministrator() {
        return !userDao.activeAdministratorExists();
    }

    public User createFirstAdministrator(String fullName, String username, String password, String contact)
            throws AuthException {
        NewAccount account = buildAccount(fullName, username, password, contact,
                Role.ADMINISTRATOR, AccountStatus.ACTIVE);
        try {
            return userDao.createFirstAdministrator(account);
        } catch (IllegalStateException e) {
            throw new AuthException(e.getMessage());
        }
    }

    /** Public registration. The new account is PENDING until an administrator activates it. */
    public User register(String fullName, String username, String password, Role role, String contact)
            throws AuthException {
        if (role == null) {
            throw new AuthException("Please choose a role.");
        }
        NewAccount account = buildAccount(fullName, username, password, contact, role, AccountStatus.PENDING);
        return userDao.register(account);
    }

    // ------------------------------------------------------------ login/logout

    public User login(String username, String password) throws AuthException {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            throw new AuthException("Username and password are required.");
        }
        User user = userDao.findByUsername(username.trim()).orElse(null);
        if (user == null || !user.verifyPassword(password)) {
            throw new AuthException("Invalid username or password."); // same message for both cases
        }
        switch (user.accountStatus()) {
            case PENDING -> throw new AuthException("Your account is still pending approval by an administrator.");
            case DEACTIVATED -> throw new AuthException("Your account has been deactivated. Please contact the barangay office.");
            default -> {
            }
        }
        activityLogDao.record(user.userId(), "LOGIN", "Users", user.userId(),
                "'" + user.username() + "' logged in");
        return user;
    }

    public void logout(User user) {
        if (user != null) {
            activityLogDao.record(user.userId(), "LOGOUT", "Users", user.userId(),
                    "'" + user.username() + "' logged out");
        }
    }

    // --------------------------------------------------- account management

    public List<User> getAllUsers(User actor) {
        return userDao.getAllUsers(actor);
    }

    public List<User> getPendingUsers(User actor) {
        return userDao.getPendingUsers(actor);
    }

    public List<User> listUsersByStatus(User actor, AccountStatus status) {
        return userDao.findByStatus(actor, status);
    }

    public void activateAccount(User actor, int targetUserId) {
        userDao.updateAccountStatus(actor, targetUserId, AccountStatus.ACTIVE);
    }

    public void approveUser(User actor, int targetUserId) {
        userDao.approveUser(actor, targetUserId);
    }

    public void deactivateAccount(User actor, int targetUserId) {
        userDao.updateAccountStatus(actor, targetUserId, AccountStatus.DEACTIVATED);
    }

    public void updateUserRoleAndStatus(User actor, int targetUserId, String newRole, String newStatus) {
        userDao.updateUserRoleAndStatus(actor, targetUserId, newRole, newStatus);
    }

    public boolean canManageUsers(User user) {
        return user != null && user.hasPermission(Permission.MANAGE_USERS);
    }

    // ---------------------------------------------------------- validation

    private NewAccount buildAccount(String fullName, String username, String password, String contact,
                                    Role role, AccountStatus status) throws AuthException {
        String cleanName = validateFullName(fullName);
        String cleanUsername = validateUsername(username);
        validatePassword(password);
        String cleanContact = validateContact(contact);
        if (userDao.usernameExists(cleanUsername)) {
            throw new AuthException("That username is already taken.");
        }
        return new NewAccount(cleanName, cleanUsername, PasswordHasher.hash(password), role, cleanContact, status);
    }

    private static String validateFullName(String fullName) throws AuthException {
        String clean = fullName == null ? "" : fullName.trim();
        if (clean.length() < 2 || clean.length() > 100) {
            throw new AuthException("Full name must be 2 to 100 characters.");
        }
        return clean;
    }

    private static String validateUsername(String username) throws AuthException {
        String clean = username == null ? "" : username.trim();
        if (!USERNAME_PATTERN.matcher(clean).matches()) {
            throw new AuthException("Username must be 4 to 20 characters: letters, numbers, or underscore only.");
        }
        return clean;
    }

    private static void validatePassword(String password) throws AuthException {
        if (password == null || password.length() < 8 || password.length() > 64) {
            throw new AuthException("Password must be 8 to 64 characters long.");
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new AuthException("Password must contain at least one letter and one number.");
        }
    }

    /** Contact details are optional; blank becomes null. */
    private static String validateContact(String contact) throws AuthException {
        String clean = contact == null ? "" : contact.trim();
        if (clean.length() > 100) {
            throw new AuthException("Contact details must be 100 characters or fewer.");
        }
        return clean.isEmpty() ? null : clean;
    }
}
