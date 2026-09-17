import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Main.java
 * Pine Labs Credit Modernization
 * PINE LABS update (via main → master):
 *   - ACTIVE customers with phone_number and pagination
 *   - Conflicts with mod-release which has INACTIVE + credit_limit
 */
public class Main {

    private static final String DB_URL      = "jdbc:postgresql://prod-db.pinelabs.com:5432/pinelabs";
    private static final String DB_DRIVER   = "org.postgresql.Driver";
    private static final String DB_USER     = "pinelabs_prod_user";
    private static final String DB_PASSWORD = "secret";

    public static void main(String[] args) throws Exception {

        Class.forName(DB_DRIVER);
        Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

        // PINE LABS: ACTIVE customers with phone_number + pagination
        // CONFLICTS with mod-release: INACTIVE + credit_limit + no pagination
        int page     = 0;
        int pageSize = 10;

        String query = """
                SELECT customer_id, COALESCE(email, 'no-email') AS email,
                       account_type, credit_limit
                FROM customers
                WHERE status = 'INACTIVE'
                ORDER BY credit_limit DESC
                LIMIT 10;
                """;

        PreparedStatement stmt = conn.prepareStatement(query);
        ResultSet rs = stmt.executeQuery();

        System.out.println("Customer ID | Email                | Account Type | Credit Limit");
        System.out.println("────────────────────────────────────────────────────────────────");

        while (rs.next()) {
            System.out.printf("%-12s | %-20s | %-12s | %s%n",
                rs.getString("customer_id"),
                rs.getString("email"),
                rs.getString("account_type"),
                rs.getString("credit_limit")
            );
        }

        rs.close();
        stmt.close();
        conn.close();
    }
}
