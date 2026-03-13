package Table;

public class LoginLogRow {

    private final int logId;
    private final String name;
    private final String email;
    private final String role;
    private final String loginAt;
    private final String logoutAt;
    private final String status;

    public LoginLogRow(int logId, String name, String email, String role,
            String loginAt, String logoutAt, String status) {
        this.logId = logId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.loginAt = loginAt;
        this.logoutAt = logoutAt;
        this.status = status;
    }

    public int getLogId() {
        return logId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getLoginAt() {
        return loginAt;
    }

    public String getLogoutAt() {
        return logoutAt;
    }

    public String getStatus() {
        return status;
    }
}
