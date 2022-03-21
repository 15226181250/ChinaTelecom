package outputformate;

import converter.DimensionConverter;
import converter.DimensionConverterImpl;
import define_type.key.CombineDimension;
import define_type.value.CountDuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.output.FileOutputCommitter;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import util.JDBCInstance;
import util.JDBCUtil;


import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author longlong
 * @create 2020 02 15 16:57
 */
public class MysqlOutputFormate extends OutputFormat<CombineDimension, CountDuration> {
    @Override
    public RecordWriter getRecordWriter(TaskAttemptContext taskAttemptContext) throws IOException, InterruptedException {
        //初始化JDBC连接对象
        Connection conn = null;
        conn = JDBCInstance.getInstance();
        try {
            //关闭自动提交，批量提交数据，减少对MySQL的操作
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            throw new RuntimeException();
        }
        return new MysqlRecordWriter(conn);
    }

    @Override
    public void checkOutputSpecs(JobContext jobContext) throws IOException, InterruptedException {
        //输出校检
    }

    @Override
    public OutputCommitter getOutputCommitter(TaskAttemptContext taskAttemptContext) throws IOException, InterruptedException {
        String name = taskAttemptContext.getConfiguration().get(FileOutputFormat.OUTDIR);
        Path output = name == null ? null : new Path(name);
        return new FileOutputCommitter(output, taskAttemptContext);
    }

    static class MysqlRecordWriter extends RecordWriter<CombineDimension,CountDuration>{
        private Connection conn = null;
        private DimensionConverter dc = new DimensionConverterImpl();
        private PreparedStatement preparedStatement = null;
        private String insertSQL = null;
        private int count = 0;
        private int maxSize = 5;

        public MysqlRecordWriter(Connection conn) {
            this.conn = conn;
        }

        @Override
        public void write(CombineDimension combineDimension, CountDuration countDuration) throws IOException, InterruptedException {

            //id_date_contact id_date_dimension id_contact call_sum call_duration_sum
            //year mouth day
            int idDateDimension = dc.getDimensionID(combineDimension.getDateDimension());
            //phone name
            int idContact = dc.getDimensionID(combineDimension.getContactDimension());
            String idDateContact = idDateDimension + "_" + idContact;
            int callSum = Integer.parseInt(countDuration.getCallSum());
            int callDurationSum = Integer.parseInt(countDuration.getCallDurationSum());
            if (insertSQL == null){
                insertSQL = "insert into ct.tb_call (id_date_contact, id_date_dimension, id_contact, call_sum, call_duration_sum) values (?,?,?,?,?) on duplicate key update id_date_contact = ?;";
            }
            try {
                if (preparedStatement == null){
                        preparedStatement = conn.prepareStatement(insertSQL);
                }
                    //本次SQL
                    int i = 0;
                    preparedStatement.setString(++i,idDateContact);
                    preparedStatement.setInt(++i,idDateDimension);
                    preparedStatement.setInt(++i,idContact);
                    preparedStatement.setInt(++i,callSum);
                    preparedStatement.setInt(++i,callDurationSum);
                    //有则更新无则插入的依据
                    preparedStatement.setString(++i,idDateContact);
                    preparedStatement.addBatch();
                    count++;
                    if (count >= maxSize){
                        preparedStatement.executeBatch();
                        conn.commit();
                        count = 0;
                        preparedStatement.clearBatch();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
            }
        }

        @Override
        public void close(TaskAttemptContext taskAttemptContext) throws IOException, InterruptedException {

            try {
                //如果不满足maxSize，最后再提交一次数据，并释放资源
                if (preparedStatement != null){
                    preparedStatement.executeBatch();
                    this.conn.commit();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                JDBCUtil.close(conn, preparedStatement, null);
            }
        }
    }
}
