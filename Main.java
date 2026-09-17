import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Main.java
 * Pine Labs Credit Modernization — Oracle to PostgreSQL migration
 * Datasource configuration and sample query execution
 */
public class Main {

    // ── Oracle datasource config (before migration) ──────────────────────────
    // private static final String DB_URL      = "jdbc:oracle:thin:@localhost:1521:orcl";
    // private static final String DB_DRIVER   = "oracle.jdbc.driver.OracleDriver";
    // private static final String DB_DIALECT  = "org.hibernate.dialect.OracleDialect";

    // ── PostgreSQL datasource config (after migration) ────────────────────────
    private static final String DB_URL      = "jdbc:postgresql://localhost:5432/pinelabs";
    private static final String DB_DRIVER   = "org.postgresql.Driver";
    private static final String DB_DIALECT  = "org.hibernate.dialect.PostgreSQLDialect";

    private static final String DB_USER     = "pinelabs_user";
    private static final String DB_PASSWORD = "secret";

    public static void main(String[] args) throws Exception {

        Class.forName(DB_DRIVER);
        Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

        // ── Oracle query (before migration) ───────────────────────────────────
        // SELECT customer_id, NVL(email, 'no-email') AS email
        // FROM customers
        // WHERE ROWNUM <= 10
        // ORDER BY created_date DESC;

        // ── PostgreSQL equivalent (after migration) ───────────────────────────
        String query = """
                SELECT customer_id, COALESCE(email, 'no-email') AS email
                FROM customers
                ORDER BY created_date DESC
                LIMIT 10;
                """;

        PreparedStatement stmt = conn.prepareStatement(query);
        ResultSet rs = stmt.executeQuery();

        System.out.println("Customer ID | Email");
        System.out.println("────────────────────────────────");

        while (rs.next()) {
            System.out.printf("%-12s | %s%n",
                rs.getString("customer_id"),
                rs.getString("email")
            );
        }

        rs.close();
        stmt.close();
        conn.close();
    }
}
