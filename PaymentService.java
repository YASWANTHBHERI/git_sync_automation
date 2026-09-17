import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * PaymentService.java
 * Pine Labs Credit Modernization
 * NEW file added by Pine Labs team on main branch.
 *
 * This file does NOT exist on master or mod-release.
 * Adding a new file guarantees a clean merge from main → master
 * with zero conflicts.
 */
public class PaymentService {

    private Connection conn;

    public PaymentService(Connection conn) {
        this.conn = conn;
    }

    // Process a payment transaction
    public String processPayment(String customerId, double amount, String currency) {
        try {
            String sql = """
                    INSERT INTO transactions (customer_id, amount, currency, status, created_at)
                    VALUES (?, ?, ?, 'PENDING', NOW())
                    """;

            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, customerId);
            stmt.setDouble(2, amount);
            stmt.setString(3, currency);
            stmt.executeUpdate();

            System.out.printf("Payment processed: customer=%s amount=%.2f currency=%s%n",
                customerId, amount, currency);

            return "SUCCESS";

        } catch (Exception e) {
            System.err.println("Payment failed: " + e.getMessage());
            return "FAILED";
        }
    }

    // Get payment status
    public String getPaymentStatus(String transactionId) {
        try {
            String sql = "SELECT status FROM transactions WHERE transaction_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, transactionId);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getString("status") : "NOT_FOUND";
        } catch (Exception e) {
            return "ERROR";
        }
    }
}
