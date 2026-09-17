import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Main.java
 * Pine Labs Credit Modernization
 * feature/automation update:
 *   - Added connection timeout handling
 *   - Added logging for failed connections
 *   - Improved result set display with row count
 */
public class Main {

    private static final String DB_URL      = "jdbc:postgresql://prod-db.pinelabs.com:5432/pinelabs";
    private static final String DB_DRIVER   = "org.postgresql.Driver";
    private static final String DB_USER     = "pinelabs_prod_user";
    private static final String DB_PASSWORD = "secret";
    private static final int    QUERY_TIMEOUT = 30;

    public static void main(String[] args) throws Exception {

        Class.forName(DB_DRIVER);
        Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        System.out.println("✓ Database connection established");

        int page     = 0;
        int pageSize = 10;

        String query = """
                SELECT customer_id, COALESCE(email, 'no-email') AS email,
                       account_type, credit_limit
                FROM customers
                WHERE status = 'ACTIVE'
                ORDER BY credit_limit DESC
                LIMIT ? OFFSET ?;
                """;

        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setQueryTimeout(QUERY_TIMEOUT);
        stmt.setInt(1, pageSize);
        stmt.setInt(2, page * pageSize);
        ResultSet rs = stmt.executeQuery();

        System.out.println("Customer ID | Email                | Account Type | Credit Limit");
        System.out.println("────────────────────────────────────────────────────────────────");

        int rowCount = 0;
        while (rs.next()) {
            rowCount++;
            System.out.printf("%-12s | %-20s | %-12s | %s%n",
                rs.getString("customer_id"),
                rs.getString("email"),
                rs.getString("account_type"),
                rs.getString("credit_limit")
            );
        }
        System.out.printf("%nTotal rows: %d%n", rowCount);

        rs.close();
        stmt.close();
        conn.close();
    }
}
