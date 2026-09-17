import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Main.java
 * Pine Labs Credit Modernization
 * Updated by MOD team — added connection pooling using HikariCP
 */
public class Main {

    // PostgreSQL datasource config — mod team added HikariCP pooling
    private static final String DB_URL      = "jdbc:postgresql://localhost:5432/pinelabs";
    private static final String DB_DRIVER   = "org.postgresql.Driver";
    private static final String DB_USER     = "pinelabs_user";
    private static final String DB_PASSWORD = "secret";
    private static final int    POOL_SIZE   = 20;  // mod team added this

    public static void main(String[] args) throws Exception {

        // mod team refactored to use HikariCP connection pool
        com.zaxxer.hikari.HikariConfig config = new com.zaxxer.hikari.HikariConfig();
        config.setJdbcUrl(DB_URL);
        config.setUsername(DB_USER);
        config.setPassword(DB_PASSWORD);
        config.setMaximumPoolSize(POOL_SIZE);

        com.zaxxer.hikari.HikariDataSource ds = new com.zaxxer.hikari.HikariDataSource(config);
        Connection conn = ds.getConnection();

        // mod team changed query — different sort order and columns
        // This CONFLICTS with master's addition of status filter
        String query = """
                SELECT customer_id, COALESCE(email, 'no-email') AS email,
                       account_type, credit_limit
                FROM customers
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
