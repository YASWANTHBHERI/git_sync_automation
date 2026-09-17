import java.sql.*;
import java.util.*;

/**
 * OrderDao.java
 * Pine Labs Credit Modernization
 * New file added by Pine Labs team — order management queries
 */
public class OrderDao {

    private Connection conn;

    public OrderDao(Connection conn) {
        this.conn = conn;
    }

    // Get orders by customer — PostgreSQL syntax
    public List<String> getOrdersByCustomer(int customerId) throws SQLException {
        String sql = """
                SELECT order_id, amount, status,
                       TO_CHAR(created_at, 'YYYY-MM-DD') AS order_date
                FROM orders
                WHERE customer_id = ?
                ORDER BY created_at DESC
                LIMIT 50;
                """;

        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, customerId);
        ResultSet rs = stmt.executeQuery();

        List<String> orders = new ArrayList<>();
        while (rs.next()) {
            orders.add(String.format("%s | %s | %s | %s",
                rs.getString("order_id"),
                rs.getString("amount"),
                rs.getString("status"),
                rs.getString("order_date")
            ));
        }
        return orders;
    }

    // Get pending orders count
    public int getPendingOrdersCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM orders WHERE status = 'PENDING'";
        PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery();
        return rs.next() ? rs.getInt(1) : 0;
    }
}
