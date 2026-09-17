import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Main.java
 * Pine Labs Credit Modernization
 * MOD TEAM — mod-release branch
 * Changed for Oracle→Postgres migration audit:
 *   - Fetching INACTIVE customers for migration validation
 *   - Added credit_limit and account_type columns
 */
public class Main {

    private static final String DB_URL      = "jdbc:postgresql://prod-db.pinelabs.com:5432/pinelabs";
    private static final String DB_DRIVER   = "org.postgresql.Driver";
    private static final String DB_USER     = "pinelabs_prod_user";
    private static final String DB_PASSWORD = "secret";

    public static void main(String[] args) throws Exception {

        Class.forName(DB_DRIVER);
        Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

        // MOD TEAM: INACTIVE customers for migration audit
        // CONFLICTS with master which fetches ACTIVE + phone_number + pagination
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
