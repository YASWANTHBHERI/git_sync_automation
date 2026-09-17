import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Main.java
 * Pine Labs Credit Modernization
 * Updated by Pine Labs team on master — added status filter and new sort order
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

        // Pine Labs team — added status=ACTIVE filter and last_login sort
        // CONFLICTS with mod-release which uses HikariCP and credit_limit sort
        String query = """
                SELECT customer_id, COALESCE(email, 'no-email') AS email,
                       status, last_login
                FROM customers
                WHERE status = 'ACTIVE'
                ORDER BY last_login DESC
                LIMIT 10;
                """;

        PreparedStatement stmt = conn.prepareStatement(query);
        ResultSet rs = stmt.executeQuery();

        System.out.println("Customer ID | Email                | Status | Last Login");
        System.out.println("────────────────────────────────────────────────────────");

        while (rs.next()) {
            System.out.printf("%-12s | %-20s | %-6s | %s%n",
                rs.getString("customer_id"),
                rs.getString("email"),
                rs.getString("status"),
                rs.getString("last_login")
            );
        }

        rs.close();
        stmt.close();
        conn.close();
    }
}
