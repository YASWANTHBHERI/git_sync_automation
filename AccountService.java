import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * AccountService.java
 * Pine Labs Credit Modernization
 * New file added via feature/automation branch
 * Account balance and status management — PostgreSQL syntax
 */
public class AccountService {

    private Connection conn;

    public AccountService(Connection conn) {
        this.conn = conn;
    }

    // Get account balance by account ID
    public double getBalance(String accountId) {
        try {
            String sql = """
                    SELECT COALESCE(balance, 0.00) AS balance
                    FROM accounts
                    WHERE account_id = ?
                    AND status = 'ACTIVE'
                    LIMIT 1;
                    """;

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, accountId);
            ResultSet rs = stmt.executeQuery();

            return rs.next() ? rs.getDouble("balance") : 0.00;

        } catch (Exception e) {
            System.err.println("Failed to fetch balance: " + e.getMessage());
            return 0.00;
        }
    }

    // Update account status
    public boolean updateStatus(String accountId, String status) {
        try {
            String sql = """
                    UPDATE accounts
                    SET status = ?, last_updated = NOW()
                    WHERE account_id = ?
                    """;

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, status);
            stmt.setString(2, accountId);

            int rows = stmt.executeUpdate();
            return rows > 0;

        } catch (Exception e) {
            System.err.println("Failed to update status: " + e.getMessage());
            return false;
        }
    }
}
