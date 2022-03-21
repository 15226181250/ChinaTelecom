package converter;

import com.mysql.jdbc.Connection;
import define_type.base.DimensionBase;
import define_type.key.ContactDimension;
import define_type.key.DateDimension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.JDBCInstance;
import util.JDBCUtil;
import util.MyLRUCache;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @Describe:根据传入的维度数据，得到该数据在表中的主键ID
 *  **做内存缓存，LRUCache
 *    分支：
 *       --缓存中有数据 -> 直接返回ID
 *       --缓存中无数据 -> 查询Mysql
 *           分支：
 *              --mysql中有该条数据 -> 直接返回id -> 将本次读取到的id缓存到内存中
 *              --mysql中没有改数据 -> 插入该数据 -> 再次反查该数据,得到id -> 缓存到内存中
 * @Author: longlong
 * @Date: 2020/2/15 20:34
 */
public class DimensionConverterImpl implements DimensionConverter {
    //logger
    private static final Logger LOGGER = LoggerFactory.getLogger(DimensionConverterImpl.class);
    //对象线程化   用于每个线程管理自己的JDBC连接器
    private ThreadLocal<Connection> threadLocalConnection = new ThreadLocal<>();
    //构建内存缓存对象
    private MyLRUCache myLRUCache = new MyLRUCache(3000);

    public DimensionConverterImpl() {
        //jvm关闭时释放资源
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                JDBCUtil.close(threadLocalConnection.get(), null, null);
            }
        }));
    }

    @Override
    public int getDimensionID(DimensionBase dimensionBase) {
        //根据传入的维度对象获取对应的主键id，先从LRUCache中获取
        //时间维度：date_dimension_year_mouth_day,10
        //联系人维度：contact_dimension_phone,12
        String cacheKey = getCacheKey(dimensionBase);
        //尝试获取缓存id
        if (myLRUCache.containsKey(cacheKey)) {
            return myLRUCache.get(cacheKey);
        }

        //没有得到缓存id，执行select操作
        //sqls包含一组数据，插入和查询
        String[] sqls = null;
        if (dimensionBase instanceof DateDimension) {
            sqls = getDateDimensionSQL();
        } else if (dimensionBase instanceof ContactDimension) {
            sqls = getContactDimension();
        } else {
            throw new RuntimeException("传入维度错误");
        }

        //准备对MySQL表进行操作，先查询有可能再插入
        Connection conn = this.getConnection();
        int id = -1;
        //添加互斥锁防止其它线程干扰
        synchronized (this){
            id = execSql(conn, sqls, dimensionBase);
        }
        //将查询到的id加入到缓存中
        myLRUCache.put(cacheKey, id);
        return id;
    }

    /**
     * @Describe:返回联系人表查询和插入语句
     * @Param: []
     * @Return: java.lang.String[]
     * @Author: longlong
     * @Date: 2020/2/15 22:15
     */
    private String[] getContactDimension() {
        String query = "select id from ct.tb_contacts where telephone = ? and name = ? order by id;";
        String insert = "insert into ct.tb_contacts (telephone, name) values (?,?);";
        return new String[]{query, insert};
    }

    /**
     * @Describe:返回时间表的查询和插入语句
     * @Param: []
     * @Return: java.lang.String[]
     * @Author: longlong
     * @Date: 2020/2/15 22:15
     */
    private String[] getDateDimensionSQL() {
        String query = "select id from ct.tb_dimension_date where year = ? and month = ? and day = ? order by id;";
        String insert = "insert into ct.tb_dimension_date (year, month, day) values (?,?,?);";
        return new String[]{query, insert};
    }

    /**
     * @Describe:根据维度信息得到对应的维度键
     * @Param: [dimensionBase]
     * @Return: java.lang.String
     * @Author: longlong
     * @Date: 2020/2/15 21:28
     */
    private String getCacheKey(DimensionBase dimensionBase) {
        StringBuilder sb = new StringBuilder();
        if (dimensionBase instanceof DateDimension) {
            DateDimension dateDimension = (DateDimension) dimensionBase;
            sb = sb.append("date_dimension")
                    .append(dateDimension.getYear())
                    .append(dateDimension.getMouth())
                    .append(dateDimension.getDay());
        } else if (dimensionBase instanceof ContactDimension) {
            ContactDimension contactDimension = (ContactDimension) dimensionBase;
            sb = sb.append("contact_dimension").append(contactDimension.getPhone());
        }
        return null;
    }
    /**
     * @Describe:得到当前线程维护的Connection对象
     * @Param: []
     * @Return: com.mysql.jdbc.Connection
     * @Author: longlong
     * @Date: 2020/2/15 22:54
     */
    private Connection getConnection() {
        Connection conn = null;
        //添加互斥锁防止其他线程干扰
        synchronized (this) {
            try {
                conn = threadLocalConnection.get();
                if (conn == null || conn.isClosed() || conn.isValid(3)) {
                    conn = (Connection)JDBCInstance.getInstance();
                    threadLocalConnection.set(conn);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return conn;
    }
    /**
     * @Describe:根据传入的信息执行SQL语句
     * @Param: [conn, sql, dimension]
     * @Return: int
     * @Author: longlong
     * @Date: 2020/2/15 23:00
     */
    private int execSql(Connection conn, String[] sqls, DimensionBase dimension){
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        int result = -1;
        try {
            // 1
            //执行查询语句
            preparedStatement = conn.prepareStatement(sqls[0]);
            //根据不同的维度封装不同的SQL语句
            setArguments(preparedStatement,dimension);
            //执行查询
            resultSet = preparedStatement.executeQuery();
            //下标的初始位置在第一行数据的前一行，如果调用next()方法为true，则返回第一行数据
            if (resultSet.next()){
                //返回第一列的数据，即id
                result = resultSet.getInt(1);
                //释放资源
                JDBCUtil.close(null,preparedStatement,resultSet);
                return result;
            }
            //释放资源
            JDBCUtil.close(null,preparedStatement,resultSet);

            //2
            //执行插入语句
            preparedStatement = conn.prepareStatement(sqls[1]);
            setArguments(preparedStatement,dimension);
            preparedStatement.executeUpdate();
            //释放资源
            JDBCUtil.close(null,preparedStatement,null);

            // 3
            //执行查询语句
            preparedStatement = conn.prepareStatement(sqls[0]);
            //根据不同的维度封装不同的SQL语句
            setArguments(preparedStatement,dimension);
            //执行查询
            resultSet = preparedStatement.executeQuery();
            //下标的初始位置在第一行数据的前一行，如果调用next()方法为true，则返回第一行数据
            if (resultSet.next()){
                //返回第一列的数据，即id
                return resultSet.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            //释放资源
            JDBCUtil.close(null,preparedStatement,resultSet);
        }
        return -1;
    }
    /**
     * @Describe:设置SQL语句的具体参数
     * @Param: [preparedStatement, dimension]
     * @Return: void
     * @Author: longlong
     * @Date: 2020/2/15 23:26
     */
    private void setArguments(PreparedStatement preparedStatement, DimensionBase dimension) {
        int i = 0;
        try {
            if (dimension instanceof DateDimension){
                //可以优化
                //根据参数下标设置对应位置参数，参数下标从1开始
                DateDimension dateDimension = (DateDimension) dimension;
                preparedStatement.setString(++i,dateDimension.getYear());
                preparedStatement.setString(++i,dateDimension.getMouth());
                preparedStatement.setString(++i,dateDimension.getDay());
            }else if(dimension instanceof ContactDimension){
                ContactDimension contactDimension = (ContactDimension) dimension;
                preparedStatement.setString(++i,contactDimension.getPhone());
                preparedStatement.setString(++i,contactDimension.getName());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
