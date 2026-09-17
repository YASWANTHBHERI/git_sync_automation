import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Main.java
 * Pine Labs Credit Modernization — Oracle to PostgreSQL migration
 * MASTER VERSION: Pine Labs production update
 */
public class Main {

    // ── MASTER: Pine Labs updated production config ───────────────────────────
    private static final String DB_URL      = "jdbc:postgresql://prod-db:5432/pinelabs_prod";
    private static final String DB_DRIVER   = "org.postgresql.Driver";
    private static final String DB_DIALECT  = "org.hibernate.dialect.PostgreSQLDialect";
    private static final String DB_SCHEMA   = "prod_schema";

    private static final String DB_USER     = "prod_user";
    private static final String DB_PASSWORD = "prod_secret";

    public static void main(String[] args) throws Exception {

        Class.forName(DB_DRIVER);
        Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

        // ── MASTER: Pine Labs updated this query differently ──────────────────
        // Added transaction_date filter and changed LIMIT to 50
        String query = """
                SELECT c.customer_id, COALESCE(c.email, 'no-email') AS email,
                       c.phone_number
                FROM prod_schema.customers c
                WHERE c.transaction_date >= NOW() - INTERVAL '30 days'
                ORDER BY c.transaction_date DESC
                LIMIT 50;
                """;

        PreparedStatement stmt = conn.prepareStatement(query);
        ResultSet rs = stmt.executeQuery();

        System.out.println("Customer ID | Email                | Phone");
        System.out.println("────────────────────────────────────────────");

        while (rs.next()) {
            System.out.printf("%-12s | %-20s | %s%n",
                rs.getString("customer_id"),
                rs.getString("email"),
                rs.getString("phone_number")
            );
        }

        rs.close();
        stmt.close();
        conn.close();
    }
}
