import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Main.java
 * Pine Labs Credit Modernization
 * Updated by Pine Labs team on master — new customer fetch logic added
 */
public class Main {

    // PostgreSQL datasource config
    private static final String DB_URL      = "jdbc:postgresql://localhost:5432/pinelabs";
    private static final String DB_DRIVER   = "org.postgresql.Driver";
    private static final String DB_USER     = "pinelabs_user";
    private static final String DB_PASSWORD = "secret";

    public static void main(String[] args) throws Exception {

        Class.forName(DB_DRIVER);
        Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

        // Fetch top 10 active customers — updated by Pine Labs team
        // Added status filter and new sort order
        String query = """
                SELECT customer_id, COALESCE(email, 'no-email') AS email, status
                FROM customers
                WHERE status = 'ACTIVE'
                ORDER BY last_login DESC
                LIMIT 10;
                """;

        PreparedStatement stmt = conn.prepareStatement(query);
        ResultSet rs = stmt.executeQuery();

        System.out.println("Customer ID | Email                | Status");
        System.out.println("──────────────────────────────────────────────");

        while (rs.next()) {
            System.out.printf("%-12s | %-20s | %s%n",
                rs.getString("customer_id"),
                rs.getString("email"),
                rs.getString("status")
            );
        }

        rs.close();
        stmt.close();
        conn.close();
    }
}
