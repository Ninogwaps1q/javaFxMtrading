package config;

import AdminController.AdminSession;
import UserController.UserSession;
import java.sql.Connection;

public final class SessionAuditUtil {

    private SessionAuditUtil() {
    }

    public static void logoutAdminSession() {
        AuditLogService.recordLogout(AdminSession.getLoginLogId());
        AdminSession.clear();
    }

    public static void logoutAdminSession(Connection conn) {
        AuditLogService.recordLogout(conn, AdminSession.getLoginLogId());
        AdminSession.clear();
    }

    public static void logoutUserSession() {
        AuditLogService.recordLogout(UserSession.getLoginLogId());
        UserSession.clear();
    }

    public static void logoutUserSession(Connection conn) {
        AuditLogService.recordLogout(conn, UserSession.getLoginLogId());
        UserSession.clear();
    }

    public static void closeAnyActiveSessions() {
        logoutUserSession();
        logoutAdminSession();
    }

    public static void closeAnyActiveSessions(Connection conn) {
        logoutUserSession(conn);
        logoutAdminSession(conn);
    }
}
