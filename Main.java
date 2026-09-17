import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Main.java
 * Pine Labs Credit Modernization — Oracle to PostgreSQL migration
 * MOD-RELEASE VERSION: Team's Postgres conversion work
 */
public class Main {

    // ── MOD-RELEASE: PostgreSQL config updated by conversion team ─────────────
    private static final String DB_URL      = "jdbc:postgresql://mod-release-db:5432/pinelabs_v2";
    private static final String DB_DRIVER   = "org.postgresql.Driver";
    private static final String DB_DIALECT  = "org.hibernate.dialect.PostgreSQLDialect";
    private static final String DB_SCHEMA   = "pinelabs_schema";

    private static final String DB_USER     = "mod_release_user";
    private static final String DB_PASSWORD = "mod_release_secret";

    public static void main(String[] args) throws Exception {

        Class.forName(DB_DRIVER);
        Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

        // ── MOD-RELEASE: Conversion team rewrote this query ───────────────────
        // Using COALESCE + LIMIT + schema prefix
        String query = """
                SELECT c.customer_id, COALESCE(c.email, 'no-email') AS email,
                       c.account_status
                FROM pinelabs_schema.customers c
                WHERE c.account_status = 'ACTIVE'
                ORDER BY c.created_date DESC
                LIMIT 25;
                """;

        PreparedStatement stmt = conn.prepareStatement(query);
        ResultSet rs = stmt.executeQuery();

        System.out.println("Customer ID | Email                | Status");
        System.out.println("────────────────────────────────────────────");

        while (rs.next()) {
            System.out.printf("%-12s | %-20s | %s%n",
                rs.getString("customer_id"),
                rs.getString("email"),
                rs.getString("account_status")
            );
        }

        rs.close();
        stmt.close();
        conn.close();
    }
}
