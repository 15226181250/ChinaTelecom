package utils;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.NamespaceDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.util.Bytes;


import java.io.IOException;
import java.text.DecimalFormat;
import java.util.TreeSet;

public class HbaseUtil {
    /**
     * @Description:判断表是否存在
     * @Param: [conf, tableName]
     * @Return: boolean
     * @Author: 谁伏特
     * @Date: 2020/2/11 20:01
     */
    public static boolean isExist(Configuration conf,String tableName) throws IOException {
        Connection conn = ConnectionFactory.createConnection(conf);
        Admin admin = conn.getAdmin();
        boolean result = admin.isTableAvailable(TableName.valueOf(tableName));
        admin.close();
        //conn.close();
        return result;

    }
    /**
     * @Description:创建命名空间
     * @Param: [conf, namespace]
     * @Return: void
     * @Author: 谁伏特
     * @Date: 2020/2/11 20:07
     */
    public static void initNameSpace(Configuration conf,String namespace) throws IOException {

        Connection conn = ConnectionFactory.createConnection(conf);
        Admin admin = conn.getAdmin();  //对表操作用Admin
        NamespaceDescriptor nd = NamespaceDescriptor.create(namespace)
                .addConfiguration("actor","shenzhenhua")
                .addConfiguration("time", String.valueOf(System.currentTimeMillis()))
                .build();
        admin.createNamespace(nd);
        admin.close();
        conn.close();
    }
    /**
     * @Description:创建表
     * @Param: [conf, tableName, columnFamily]
     * @Return: void
     * @Author: 谁伏特
     * @Date: 2020/2/11 20:13
     */
    public static void createTable(Configuration conf,String tableName,int splitNum,
                                   String... columnFamily) throws IOException {
        Connection conn = ConnectionFactory.createConnection(conf);
        Admin admin = conn.getAdmin();  //对表操作用Admin
        if (isExist(conf,tableName)) return;
        //创建用户关系表描述器
        HTableDescriptor htd = new HTableDescriptor(TableName.valueOf(tableName));
        for (String family: columnFamily){
            HColumnDescriptor hcd = new HColumnDescriptor(family);
            //设置块缓存
            hcd.setBlockCacheEnabled(true);
            //设置块缓存大小
            hcd.setBlocksize(2*1024*1024);
            //设置版本确界
            //hcd.setMinVersions(1);
            //hcd.setMaxVersions(1);
            //将列描述器添加到表描述器中
            htd.addFamily(hcd);
        }
        //添加协处理器
        //htd.addCoprocessor("hbase.CalleeWriteObserver");
        //根据分区规则创建表
        admin.createTable(htd, getSplitKeys(splitNum));
        admin.close();
        conn.close();
    }
    /**
     * @Description:创建分区规则，生成分区键
     * @Param: [regions]
     * @Return: byte[][]
     * @Author: 谁伏特
     * @Date: 2020/2/11 21:09
     */
    public static byte[][] getSplitKeys(int regions){
        //定义一个存放String类型的分区键的数组
        String[] keys = new String[regions];
        //目前推算，region的个数不会超过两位数，所以region分区键格式化为两位数字代表的分区键
        DecimalFormat df = new DecimalFormat("00");
        for (int i=0;i<regions;i++){
            //将所有的分区键格式化为(00|),"|"符号在ASCLL码表中最大，用来做界限，即比较到第三位时一定会停止
            keys[i] = df.format(i)+"|";
        }
        //定义一个存放byre[]类型的分区键的数组
        byte[][] splitKey = new byte[regions][];
        //生成byre[]类型的分区键的时候，一定要保证有序规则
        TreeSet<byte[]> sortKeys = new TreeSet<byte[]>(Bytes.BYTES_COMPARATOR);
        for (String key : keys){
            sortKeys.add(Bytes.toBytes(key));
        }
        int count=0;
        //将排完序的分区键放到byre[]类型的分区键数组中
        for (byte[] sortKey : sortKeys){
            splitKey[count++] = sortKey;
        }
        return splitKey;
    }
    /**
     * @Description:生成rowkey
     * regionCode_call1_buildTime_call2_flag_duration
     * @Param: [regionCode, call1, buildTime, call2, flag, duration]
     * @Return: java.lang.String
     * @Author: 谁伏特
     * @Date: 2020/2/11 22:03
     */
    public static String getRowKey(String regionCode,String call1,String buildTime,
                                   String call2,String flag,String duration){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(regionCode+"_")
                .append(call1+"_")
                .append(buildTime+"_")
                .append(call2+"_")
                .append(flag+"_")
                .append(duration);
        return stringBuilder.toString();
    }
    /**
     * @Description:生成分区号
     * @Param: [call1, bulidTIme, regions]
     * @Return: java.lang.String
     * @Author: 谁伏特
     * @Date: 2020/2/11 22:49
     */
    public static String getRegionCode(String call1,String bulidTIme,int regions){
        int length = call1.length();
        //取出最后4位号码
        String lastPhone = call1.substring(length-4);
        //取出年月(只精确到月，保证同一个用户每个月的通话记录在一个分区里面，便于后期用
        // startRow和stopRow范围查找，提升查找速度)
        String yearMouth = bulidTIme.replaceAll("-","")
                .replaceAll(":","")
                .replaceAll(" ","")
                .substring(0,6);
        //离散操作1
        Integer x = Integer.parseInt(lastPhone) ^ Integer.parseInt(yearMouth);
        //离散操作2
        int y = x.hashCode();
        //生成分区号
        int regionCode = y % regions;
        //格式化分区号
        DecimalFormat df = new DecimalFormat("00");
        return df.format(regionCode);
    }

}
