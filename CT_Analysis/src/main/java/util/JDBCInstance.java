package util;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author longlong
 * @create 2020 02 14 23:34
 */
public class JDBCInstance {
    private static Connection conn = null;
    private JDBCInstance(){}
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
