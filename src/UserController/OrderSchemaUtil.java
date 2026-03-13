package UserController;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class OrderSchemaUtil {

    private OrderSchemaUtil() {
    }

    public static void ensurePaymentColumns(Connection conn) throws SQLException {
        addColumnIfMissing(conn, "tbl_orders", "payment_method", "TEXT");
        addColumnIfMissing(conn, "tbl_orders", "payment_ref", "TEXT");
        addColumnIfMissing(conn, "tbl_orders", "gross_total", "REAL");
        addColumnIfMissing(conn, "tbl_orders", "discount_amount", "REAL");
        addColumnIfMissing(conn, "tbl_orders", "voucher_code", "TEXT");
    }

    private static void addColumnIfMissing(Connection conn, String table, String column, String type) throws SQLException {
        String checkSql = "PRAGMA table_info(" + table + ")";
        try (Statement checkStmt = conn.createStatement();
             ResultSet rs = checkStmt.executeQuery(checkSql)) {

            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    return;
                }
            }
        }

        String alter = "ALTER TABLE " + table + " ADD COLUMN " + column + " " + type;
        try (Statement alterStmt = conn.createStatement()) {
            alterStmt.execute(alter);
        }
    }
}
