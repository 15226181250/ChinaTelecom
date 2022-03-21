import aaa.MyLRUCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.LRUCache;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 维度对象转维度 id 类 
 *
 * @author 尽际
 */
public class DimensionConverter implements IConverter {
    //日志记录类，注意导包的正确性 
    private static final Logger logger = LoggerFactory.getLogger(DimensionConverter.class);
    //每个线程保留自己的 Connection 实例 
    private ThreadLocal<Connection> threadLocalConnection = new ThreadLocal<>();

    //创建数据缓存队列
    private MyLRUCache lruCache = new MyLRUCache(3000);
    //private LRUCache<String,Integer> lruCache = new MyLRUCache<String,Integer>(3000);

    public DimensionConverter() {
         // JVM 虚拟机关闭时，尝试关闭数据库连接
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {

                    JDBCUtil.close(threadLocalConnection.get(), null, null);

            }
        }));
    }

    /**
     * 1、判断内存缓存中是否已经有该维度的 id，如果存在则直接返回该 id 
     * 2、如果内存缓存中没有，则查询数据库中是否有该维度 id，如果有，则查询出来，
     返回该 id，并缓存到内存中。
     * 3、如果数据库中也没有该维度 id，则直接插入一条新的维度信息，成功插入后，重
     新查询该维度，返回该 id，并缓存到内存中。
     *
     * @param dimension
     * @return
     */
    @Override
    public int getDimensionId(BaseDimension dimension) throws IOException {
        //1、根据传入的维度对象取得该维度对象对应的 cachekey 
        String cacheKey = genCacheKey(dimension);
        //2、判断缓存中是否存在该 cacheKey 的缓存 
        if (lruCache.containsKey(cacheKey)) {
            return (int) lruCache.get(cacheKey);
        }

        //3、缓存中没有，查询数据库 
      String[] sqls = null;
        if (dimension instanceof DateDimension) {
            // 时间维度表 tb_dimension_date 
            sqls = genDateDimensionSQL();
        } else if (dimension instanceof ContactDimension) {
            //联系人表 tb_contacts 
            sqls = genContactSQL();
        } else {
            //抛出 Checked 异常，提醒调用者可以自行处理。 
            throw new IOException("Cannot match the dimession, unknown dimension.");
        }

        try {
            Connection conn = this.getConnection();
            int id = -1;
            synchronized (this) {
                id = execSQL(conn, sqls, dimension);
            }
            //将该 id 缓存到内存中 
            lruCache.put(cacheKey, id);
            return id;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IOException(e);
        }
    }

    /**
     尚硅谷大数据技术之电信客服 —————————————————————————————
     【更多 Java、HTML5、Android、Python、大数据 资料下载，可访问尚硅谷（中国）官 网 www.atguigu.com 下载区】
     * LRUCACHE 中缓存的键值对形式例如：<date_dimension20170820, 3> 
     *
     * @param dimension
     * @return
     */
    private String genCacheKey(BaseDimension dimension) {
        StringBuilder sb = new StringBuilder();
        if (dimension instanceof DateDimension) {
            DateDimension dateDimension = (DateDimension) dimension;
            //拼装缓存 id 对应的 key 
            sb.append("date_dimension");

            sb.append(dateDimension.getYear()).append(dateDimension.getMonth()).append(dateDimension.
                    getDay());
        } else if (dimension instanceof ContactDimension) {
            ContactDimension contactDimension = (ContactDimension) dimension;
            //拼装缓存 id 对应的 key 
            sb.append("contact_dimension");

            sb.append(contactDimension.getTelephone()).append(contactDimension.getName());
        }

        if (sb.length() <= 0) throw new RuntimeException("Cannot create cachekey." +
                dimension);

        return sb.toString();
    }

   /**
     * 生成时间维度的数据库查询语句和插入语句 
     *
     * @return
     */
    private String[] genDateDimensionSQL() {
        String query = "SELECT `id` FROM `tb_dimension_date` WHERE `year` = ? AND `month` = ? AND `day` = ? order by `id`;";
        String insert = "INSERT INTO `tb_dimension_date`(`year`, `month`, `day`) VALUES(?, ?, ?);";
        return new String[]{query, insert};
    }

    /**
     * 生成联系人的数据库查询语句和插入语句 
     *
     * @return
     */
    private String[] genContactSQL() {
        String query = "SELECT `id` FROM `tb_contacts` WHERE `telephone` = ? AND `name` = ? order by `id`;";
        String insert = "INSERT INTO `tb_contacts`(`telephone`, `name`) VALUES(?, ?);";
        return new String[]{query, insert};
    }

    /**
     * 尝试获取数据库连接对象：先从线程缓冲中获取，没有可用连接则创建。 
     *
       * @return
     * @throws SQLException
     */
    private Connection getConnection() throws SQLException {
        Connection conn = null;
        synchronized (this) {
            conn = threadLocalConnection.get();
            if (conn == null || conn.isClosed() || conn.isValid(3)) {
                conn = JDBCCacheBean.getInstance();
            }
            threadLocalConnection.set(conn);
        }
        return conn;
    }

    /**
     * @param conn
     * @param sqls      第一个为查询语句，第二个为插入语句 
     * @param dimension
     * @return
     */
    private int execSQL(Connection conn, String[] sqls, BaseDimension dimension) throws
            SQLException {
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            //1、假设数据库中有该条数据 
            //封装查询 sql 语句 
          preparedStatement = conn.prepareStatement(sqls[0]);
            setArguments(preparedStatement, dimension);

            //执行查询 
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }

            //2、假设 数据库中没有该条数据 
            //封装插入 sql 语句 
            preparedStatement = conn.prepareStatement(sqls[1]);
            setArguments(preparedStatement, dimension);
            preparedStatement.executeUpdate();
            JDBCUtil.close(null, preparedStatement, resultSet);

            //重新获取 id，调用自己即可。 
            preparedStatement = conn.prepareStatement(sqls[0]);
            setArguments(preparedStatement, dimension);
            //执行查询 
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        } finally {
            JDBCUtil.close(null, preparedStatement, resultSet);
        }
        throw new RuntimeException("Failed to get id");
  }

    private void setArguments(PreparedStatement preparedStatement, BaseDimension
            dimension) throws SQLException {
        int i = 0;
        if (dimension instanceof DateDimension) {
            DateDimension dateDimension = (DateDimension) dimension;
            preparedStatement.setInt(++i, dateDimension.getYear());
            preparedStatement.setInt(++i, dateDimension.getMonth());
            preparedStatement.setInt(++i, dateDimension.getDay());
        } else if (dimension instanceof ContactDimension) {
            ContactDimension contactDimension = (ContactDimension) dimension;
            preparedStatement.setString(++i, contactDimension.getTelephone());
            preparedStatement.setString(++i, contactDimension.getName());
        }
    }
} 