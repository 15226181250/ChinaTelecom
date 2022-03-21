package hbase;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Durability;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.coprocessor.BaseRegionObserver;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.coprocessor.RegionCoprocessorEnvironment;
import org.apache.hadoop.hbase.regionserver.wal.WALEdit;
import org.apache.hadoop.hbase.util.Bytes;
import utils.HbaseUtil;
import utils.PropertiesUtil;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class CalleeWriteObserver extends BaseRegionObserver {
    @Override
    public void postPut(ObserverContext<RegionCoprocessorEnvironment> e, Put put, WALEdit edit, Durability durability) throws IOException {
        super.postPut(e, put, edit, durability);
        //获取你想要操作的目标表的名称
        String targetTableName = PropertiesUtil.getProperty("hbase.calllog.tableName");
        //获取当前成功Put了数据的表(不一定是我们当前业务想要操作的表)
        String currentTableName = e.getEnvironment().getRegion().getRegionInfo().getTable().getNameAsString();
        if (!targetTableName.equals(currentTableName)) return;

        String rowkey = Bytes.toString(put.getRow());
        String[] splits = rowkey.split("_");
        String flag = splits[4];
        //如果当前插入的是被叫数据，则直接返回(因为默认提供的数据全部为主叫数据)
        if(flag.equals("0")) return;
        //当前插入的数据描述
        String caller = splits[1];
        String callee = splits[3];
        String buildTime = splits[2];
        String duration = splits[5];
        String timestamp = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            timestamp = String.valueOf(sdf.parse(buildTime).getTime());
        } catch (ParseException e1) {
            e1.printStackTrace();
        }

        //组装新的数据所在分区号
        int regions = Integer.valueOf(PropertiesUtil.getProperty("hbase.regions.count"));
        String regionCode = HbaseUtil.getRegionCode(callee, buildTime, regions);
        String newFlag = "0";
        String rowKey = HbaseUtil.getRowKey(regionCode, callee, buildTime, caller, newFlag, duration);

        //开始存放被叫数据
        Put newPut = new Put(Bytes.toBytes(rowKey));

        newPut.addColumn(Bytes.toBytes("info2"), Bytes.toBytes("call1"), Bytes.toBytes(callee));
        newPut.addColumn(Bytes.toBytes("info2"), Bytes.toBytes("call2"), Bytes.toBytes(caller));
        newPut.addColumn(Bytes.toBytes("info2"), Bytes.toBytes("build_time"), Bytes.toBytes(buildTime));
        newPut.addColumn(Bytes.toBytes("info2"), Bytes.toBytes("build_time_ts"),
                Bytes.toBytes(timestamp));
        newPut.addColumn(Bytes.toBytes("info2"), Bytes.toBytes("duration"), Bytes.toBytes(duration));
        newPut.addColumn(Bytes.toBytes("info2"), Bytes.toBytes("flag"), Bytes.toBytes(newFlag));

        Table hTable = e.getEnvironment().getTable(TableName.valueOf(targetTableName));
        hTable.put(newPut);
        //hTable.close();
    }
}
