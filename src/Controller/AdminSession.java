package Controller;

public class AdminSession {

    private static String name;
    private static String email;
    private static String role;

    public static void setAdmin(String name, String email, String role) {
        AdminSession.name = name;
        AdminSession.email = email;
        AdminSession.role = role;
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

    public static void clear() {
        name = null;
        email = null;
        role = null;
    }
}
