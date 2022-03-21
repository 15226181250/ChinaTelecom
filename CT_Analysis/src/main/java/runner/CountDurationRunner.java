package runner;

import define_type.key.CombineDimension;
import define_type.value.CountDuration;
import mapper.CountDurationMapper;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import outputformate.MysqlOutputFormate;
import reducer.CountDurationReducer;

import java.io.IOException;

/**
 * @author longlong
 * @create 2020 02 15 14:40
 */
public class CountDurationRunner implements Tool {

    private Configuration conf = null;
    @Override
    public void setConf(Configuration configuration) {
        this.conf=HBaseConfiguration.create(configuration);
    }

    @Override
    public Configuration getConf() {
        return this.conf;
    }

    private void initHBaseInputConfig(Job job) {
        Connection conn = null;
        Admin admin = null;
        try {
            String tableName = "calllog";
            conn = ConnectionFactory.createConnection(conf);
            admin = conn.getAdmin();
            if (!admin.tableExists(TableName.valueOf(tableName))) throw new RuntimeException("无法找到目标");
            Scan scan = new Scan();
            //可以优化
            //初始化Mapper
            TableMapReduceUtil.initTableMapperJob(tableName,
                    scan,
                    CountDurationMapper.class,
                    CombineDimension.class,
                    Text.class,
                    job,
                    true);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {

                try {
                    if(admin != null){
                        admin.close();
                    }
                    /*if(conn != null && conn.isClosed()){
                        conn.close();
                    }*/
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    private void initReducerOutputConfig(Job job) {

        job.setReducerClass(CountDurationReducer.class);
        job.setOutputKeyClass(CombineDimension.class);
        job.setOutputValueClass(CountDuration.class);
        job.setOutputFormatClass(MysqlOutputFormate.class);
    }
    @Override
    public int run(String[] strings) throws Exception {
        //实例化Job
        Job job = Job.getInstance(conf);
        job.setJarByClass(CountDurationRunner.class);
        //组装Mapper InputFormat
        initHBaseInputConfig(job);
        //组装Reducer OutputFormat
        initReducerOutputConfig(job);
        return job.waitForCompletion(true) ? 0 : 1;
    }
    public static void main(String[] args) {
        try {
            int status = ToolRunner.run(new CountDurationRunner(),args);
            System.exit(status);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
