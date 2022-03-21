package kafka;

import hbase.HbaseDao;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import utils.PropertiesUtil;

import java.util.Arrays;

public class HBaseConsumer {


    public static void main(String[] args) {

        // 定义consumer
        final KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(PropertiesUtil.properties);

        //关闭资源
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                if (consumer != null){
                    consumer.close();
                }
            }
        }));

        // 消费者订阅的topic, 可同时订阅多个
        consumer.subscribe(Arrays.asList(PropertiesUtil.getProperty("kafka.topics")));

        while (true) {
            // 读取数据，读取超时时间为100ms
            ConsumerRecords<String, String> records = consumer.poll(100);
            HbaseDao hbaseDao = new HbaseDao();
            for (ConsumerRecord<String, String> record : records){
                String line = record.value();
                System.out.println(line);
                hbaseDao.put(line);
            }
        }
    }
}
