// DBConnection.java
import java.sql.*;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost/library_db";
    private static final String USER = "root";
    private static final String PASSWORD = "krish";

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}