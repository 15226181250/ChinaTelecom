
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.Text;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CountDurationMapper extends TableMapper<ComDimension, Text>{
    //存放联系人电话与姓名的映射
    private Map<String, String> contacts;
    private byte[] family = Bytes.toBytes("f1");
    private ComDimension comDimension = new ComDimension();
    private void initContact(){
        contacts = new HashMap<String, String>();
        contacts.put("15369468720", "李雁");
        contacts.put("19920860202", "卫艺");
        contacts.put("18411925860", "仰莉");
        contacts.put("14473548449", "陶欣悦");
        contacts.put("18749966182", "施梅梅");
        contacts.put("19379884788", "金虹霖");
        contacts.put("19335715448", "魏明艳");
       contacts.put("18503558939", "华贞");
        contacts.put("13407209608", "华啟倩");
        contacts.put("15596505995", "仲采绿");
        contacts.put("17519874292", "卫丹");
        contacts.put("15178485516", "戚丽红");
        contacts.put("19877232369", "何翠柔");
        contacts.put("18706287692", "钱溶艳");
        contacts.put("18944239644", "钱琳");
        contacts.put("17325302007", "缪静欣");
        contacts.put("18839074540", "焦秋菊");
        contacts.put("19879419704", "吕访琴");
        contacts.put("16480981069", "沈丹");
        contacts.put("18674257265", "褚美丽");
    }

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        initContact();
    }

    @Override
    protected void map(ImmutableBytesWritable key, Result value, Context context) throws
            IOException, InterruptedException {
        //01_15837312345_20170810141024_13738909097_1_0180
        String rowKey = Bytes.toString(value.getRow());
        String[] values = rowKey.split("_");
        String flag = values[4];
        //只拿到主叫数据即可
       if(StringUtils.equals(flag, "0")) return;

        String date_time = values[2];
        String duration = values[5];

        String call1 = values[1];
        String call2 = values[3];

        int year = Integer.valueOf(date_time.substring(0, 4));
        int month = Integer.valueOf(date_time.substring(4, 6));
        int day = Integer.valueOf(date_time.substring(6, 8));

        DateDimension dateDimensionYear = new DateDimension(year, -1, -1);
        DateDimension dateDimensionMonth = new DateDimension(year, month, -1);
        DateDimension dateDimensionDay = new DateDimension(year, month, day);
        //第一个电话号码
        ContactDimension contactDimension1 = new ContactDimension(call1, contacts.get(call1));
        comDimension.setContactDimension(contactDimension1);

        comDimension.setDateDimension(dateDimensionYear);
        context.write(comDimension, new Text(duration));

        comDimension.setDateDimension(dateDimensionMonth);
        context.write(comDimension, new Text(duration));

        comDimension.setDateDimension(dateDimensionDay);
        context.write(comDimension, new Text(duration));
        //第二个电话号码
        ContactDimension contactDimension2 = new ContactDimension(call2,
                contacts.get(call2));
        comDimension.setContactDimension(contactDimension2);

        comDimension.setDateDimension(dateDimensionYear);
        context.write(comDimension, new Text(duration));

        comDimension.setDateDimension(dateDimensionMonth);
        context.write(comDimension, new Text(duration));

        comDimension.setDateDimension(dateDimensionDay);
        context.write(comDimension, new Text(duration));
    }
}
