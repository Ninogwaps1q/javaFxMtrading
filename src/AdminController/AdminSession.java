package AdminController;

public class AdminSession {

    private static int id;
    private static String name;
    private static String email;
    private static String role;
    private static int loginLogId;

    public static void setAdmin(int id, String name, String email, String role, int loginLogId) {
        AdminSession.id = id;
        AdminSession.name = name;
        AdminSession.email = email;
        AdminSession.role = role;
        AdminSession.loginLogId = loginLogId;
    }

    public static int getId() {
        return id;
    }

    public static String getName() {
        return name;
    }

    public static String getEmail() {
        return email;
    }

    public static String getRole() {
        return role;
    }

    public static int getLoginLogId() {
        return loginLogId;
    }

    public static void clear() {
        id = 0;
        name = null;
        email = null;
        role = null;
        loginLogId = 0;
    }
}
