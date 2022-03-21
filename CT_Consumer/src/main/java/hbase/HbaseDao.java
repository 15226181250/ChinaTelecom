package hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import utils.HbaseUtil;
import utils.PropertiesUtil;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class HbaseDao {

    private int regions;
    private String nameSpace;
    private String tableName;
    public static final Configuration conf;
    private HTable table;
    private String columnFamily1;
    private String columnFamily2;
    private Connection conn;
    private SimpleDateFormat sdf1=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private SimpleDateFormat sdf2=new SimpleDateFormat("yyyyMMddHHmmss");
    //设置一个集合本地存放Put,用于批量一次性提交所有缓存Put到Hbase集群
    private List<Put> cacheList;

    static {
        conf = HBaseConfiguration.create();

    }

    public HbaseDao(){

        cacheList = new ArrayList<Put>();
        regions = Integer.parseInt(PropertiesUtil.getProperty("hbase.calllog.regions"));
        nameSpace = PropertiesUtil.getProperty("hbase.calllog.namespace");
        tableName = PropertiesUtil.getProperty("hbase.calllog.tableName");
        columnFamily1= PropertiesUtil.getProperty("hbase.calllog.columnFamily1");
        columnFamily2= PropertiesUtil.getProperty("hbase.calllog.columnFamily2");

        /*try {
            if (!HbaseUtil.isExist(conf,tableName)){
                //HbaseUtil.initNameSpace(conf,nameSpace);
                HbaseUtil.createTable(conf,tableName,regions,columnFamily1,columnFamily2);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }
    /**
     * @Description:
     * line数据样式：18503558939,19379884788,2017-07-01 16:36:03,0548
     * rokey数据样式：02_13569074725_20200102231856_13552230628_1_1395
     * Hbase中calllog表的列{caller  callee  buildTime   buildTimeTS flag    duration}
     * @Param: [line]
     * @Return: void
     * @Author: 谁伏特
     * @Date: 2020/2/11 23:55
     */
    public void put(String line){
        try {

            if(cacheList.size() == 0){
                conn = ConnectionFactory.createConnection(conf);
                table = (HTable)conn.getTable(TableName.valueOf(tableName));
                //提交方式为设置手动提交
                table.setAutoFlush(false,true);
                //设置本地缓存多少数据，再手动提交
                table.setWriteBufferSize(2*1024*1024);
            }

            //主叫
            String[] splitLine =  line.split(",");
            String caller = splitLine[0];
            String callee = splitLine[1];
            String buildTime = sdf2.format(sdf1.parse(splitLine[2]));
            String duration = splitLine[3];
            String regionCode = HbaseUtil.getRegionCode(caller,buildTime,regions);
            String buildTimeTS = String.valueOf(sdf1.parse(splitLine[2]).getTime());
            //生成主叫rowkey
            String rowkey = HbaseUtil.getRowKey(regionCode,caller,buildTime,callee,"1",duration);

            //被叫

            String caller2 = splitLine[1];
            String callee2 = splitLine[0];
            String regionCode2 = HbaseUtil.getRegionCode(caller2,buildTime,regions);
            //生成被叫rowkey
            String rowkey2 = HbaseUtil.getRowKey(regionCode2,caller2,buildTime,callee2,"0",duration);

            //向表中添加主叫数据
            Put put = new Put(Bytes.toBytes(rowkey));
            put.addColumn(Bytes.toBytes(columnFamily1),Bytes.toBytes("call1"),Bytes.toBytes(caller));
            put.addColumn(Bytes.toBytes(columnFamily1),Bytes.toBytes("call2"),Bytes.toBytes(callee));
            put.addColumn(Bytes.toBytes(columnFamily1),Bytes.toBytes("build_time"),Bytes.toBytes(buildTime));
            put.addColumn(Bytes.toBytes(columnFamily1),Bytes.toBytes("build_time_ts"),Bytes.toBytes(buildTimeTS));
            put.addColumn(Bytes.toBytes(columnFamily1),Bytes.toBytes("flag"),Bytes.toBytes("1"));
            put.addColumn(Bytes.toBytes(columnFamily1),Bytes.toBytes("duration"),Bytes.toBytes(duration));

            //向表中添加被叫数据
            Put put2 = new Put(Bytes.toBytes(rowkey2));
            put2.addColumn(Bytes.toBytes(columnFamily2),Bytes.toBytes("call1"),Bytes.toBytes(caller2));
            put2.addColumn(Bytes.toBytes(columnFamily2),Bytes.toBytes("call2"),Bytes.toBytes(callee2));
            put2.addColumn(Bytes.toBytes(columnFamily2),Bytes.toBytes("build_time"),Bytes.toBytes(buildTime));
            put2.addColumn(Bytes.toBytes(columnFamily2),Bytes.toBytes("build_time_ts"),Bytes.toBytes(buildTimeTS));
            put2.addColumn(Bytes.toBytes(columnFamily2),Bytes.toBytes("flag"),Bytes.toBytes("0"));
            put2.addColumn(Bytes.toBytes(columnFamily2),Bytes.toBytes("duration"),Bytes.toBytes(duration));

            cacheList.add(put);
            cacheList.add(put2);

            //cacheList的大小不能超过table.setWriteBufferSize(2*1024*1024);的大小
            if (cacheList.size() >= 20){
                table.put(cacheList);
                table.flushCommits();
                table.close();
                //conn.close();
                //每提交一次清理一下cacheList
                cacheList.clear();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        finally {
            try {
                table.close();
                //conn.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
