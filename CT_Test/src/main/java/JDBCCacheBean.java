import java.sql.Connection;
import java.sql.SQLException;

public class JDBCCacheBean {
    private static Connection conn = null;
    private JDBCCacheBean(){}
    public static Connection getInstance(){
        try {
            if(conn == null || conn.isClosed() || conn.isValid(3)){
                 conn = JDBCUtil.getConnection();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }
} 