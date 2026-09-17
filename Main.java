import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Main.java
 * Pine Labs Credit Modernization
 * PINE LABS direct commit on master:
 * - Added phone_number, pagination
 * - WHERE status = 'ACTIVE'
 * - ORDER BY last_login DESC
 * This directly conflicts with mod-release changes on same lines
 */
public class Main {

    // PostgreSQL datasource config
    private static final String DB_URL      = "jdbc:postgresql://prod-db.pinelabs.com:5432/pinelabs";
    private static final String DB_DRIVER   = "org.postgresql.Driver";
    private static final String DB_USER     = "pinelabs_prod_user";
    private static final String DB_PASSWORD = "secret";

    public static void main(String[] args) throws Exception {

        Class.forName(DB_DRIVER);
        Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

        // PINE LABS — phone_number + pagination
        // CONFLICTS with mod-release: account_type + credit_limit + INACTIVE
        int page     = 0;
        int pageSize = 10;

        String query = """
                SELECT customer_id, COALESCE(email, 'no-email') AS email,
                       status, last_login, phone_number
                FROM customers
                WHERE status = 'ACTIVE'
                ORDER BY last_login DESC
                LIMIT ? OFFSET ?;
                """;

        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, pageSize);
        stmt.setInt(2, page * pageSize);
        ResultSet rs = stmt.executeQuery();

        System.out.println("Customer ID | Email                | Status | Last Login  | Phone");
        System.out.println("──────────────────────────────────────────────────────────────────");

        while (rs.next()) {
            System.out.printf("%-12s | %-20s | %-6s | %-11s | %s%n",
                rs.getString("customer_id"),
                rs.getString("email"),
                rs.getString("status"),
                rs.getString("last_login"),
                rs.getString("phone_number")
            );
        }

        rs.close();
        stmt.close();
        conn.close();
    }
}
