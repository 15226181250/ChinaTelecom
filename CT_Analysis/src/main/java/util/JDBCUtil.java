package util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * @author longlong
 * @create 2020 02 14 23:29
 */
public class JDBCUtil {

    private static final Logger logger = LoggerFactory.getLogger(JDBCUtil.class);
    private static final String MYSQL_DRIVER_CLASS = "com.mysql.jdbc.Driver";
    private static final String MYSQL_URL = "jdbc:mysql://hd003:3306/?useUnicode=true&characterEncoding=UTF-8";
    private static final String MYSQL_USERNAME = "root";
    private static final String MYSQL_PASSWORD = "123456";
    /**
     * 获取 Mysql 数据库的连接
     * @return
     */
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName(MYSQL_DRIVER_CLASS);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return DriverManager.getConnection(MYSQL_URL, MYSQL_USERNAME,
                MYSQL_PASSWORD);
    }

    /**
     * 关闭数据库连接释放资源
     * @param connection
     * @param statement
     * @param resultSet
     */
    public static void close(Connection connection, Statement statement, ResultSet resultSet){
        if(resultSet != null) try {
            resultSet.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if(statement != null) try {
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if(connection != null) try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
