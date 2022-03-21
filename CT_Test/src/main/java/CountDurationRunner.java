
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.hbase.HBaseConfiguration; 
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;

public class CountDurationRunner implements Tool{
    private Configuration conf = null;
    @Override
    public void setConf(Configuration conf) {
        this.conf = HBaseConfiguration.create(conf);
    }

    @Override
    public Configuration getConf() {
        return this.conf;
    }

    @Override
    public int run(String[] args) throws Exception {
        //得到 conf 对象 
        Configuration conf = this.getConf();
         Job job = Job.getInstance(conf, "CALL_LOG_ANALYSIS");
        job.setJarByClass(CountDurationRunner.class);
        //为 Job 设置 Mapper 
        this.setHBaseInputConfig(job);
        //为 Job 设置 Reducer 
        job.setReducerClass(CountDurationReducer.class);
        job.setOutputKeyClass(ComDimension.class);
        job.setOutputValueClass(CountDurationValue.class);
        //为 Job 设置 OutputFormat 
        job.setOutputFormatClass(MySQLOutputFormat.class);
        return job.waitForCompletion(true) ? 0 : 1;
    }

    private void setHBaseInputConfig(Job job) {
        Configuration conf = job.getConfiguration();
        HBaseAdmin admin = null;
        try {
            admin = new HBaseAdmin(conf);
            //如果表不存在则直接返回，抛个异常也挺好 
            if(!admin.tableExists("calllog")) throw new
                    RuntimeException("Unable to find the specified table.");

            Scan scan = new Scan();
            scan.setAttribute(Scan.SCAN_ATTRIBUTES_TABLE_NAME,
                    Bytes.toBytes("calllog"));
            TableMapReduceUtil.initTableMapperJob("calllog", scan,
                    CountDurationMapper.class, ComDimension.class, Text.class,
                    job, true);
      } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(admin != null) try {
                admin.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        try {
            int status = ToolRunner.run(new CountDurationRunner(), args);
            System.exit(status);
            if(status == 0){
                System.out.println("运行成功");
            }else {
                System.out.println("运行失败");
            }
        } catch (Exception e) {
            System.out.println("运行失败");
            e.printStackTrace();
        }
    }
} 