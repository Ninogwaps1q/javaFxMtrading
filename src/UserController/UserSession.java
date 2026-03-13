package UserController;

public class UserSession {
    private static int id;
    private static String name;
    private static String email;
    private static String role;
    private static int loginLogId;

    public static void set(int userId, String userName, String userEmail, int auditLogId) {
        id = userId;
        name = userName;
        email = userEmail;
        role = "User";
        loginLogId = auditLogId;
    }
    public static int getId() { return id; }
    public static String getName() { return name; }
    public static String getEmail() { return email; }
    public static String getRole() { return role; }
    public static int getLoginLogId() { return loginLogId; }
    public static void clear() {
        id = 0;
        name = null;
        email = null;
        role = null;
        loginLogId = 0;
    }
}

