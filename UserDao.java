import java.sql.*;
import java.util.*;

/**
 * UserDao.java
 * Pine Labs Credit Modernization
 * Updated by Pine Labs team — added new getUsersByRole method
 */
public class UserDao {

    private Connection conn;

    public UserDao(Connection conn) {
        this.conn = conn;
    }

    // Existing method — get user by ID
    public String getUserById(int userId) throws SQLException {
        // PostgreSQL query
        String sql = "SELECT username FROM users WHERE user_id = ? LIMIT 1";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, userId);
        ResultSet rs = stmt.executeQuery();
        return rs.next() ? rs.getString("username") : null;
    }

    // New method added by Pine Labs team on master
    // Gets all users by role with pagination
    public List<String> getUsersByRole(String role, int page, int pageSize) throws SQLException {
        String sql = """
                SELECT username FROM users
                WHERE role = ?
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
                """;
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, role);
        stmt.setInt(2, pageSize);
        stmt.setInt(3, page * pageSize);

        ResultSet rs = stmt.executeQuery();
        List<String> users = new ArrayList<>();
        while (rs.next()) {
            users.add(rs.getString("username"));
        }
        return users;
    }
}
