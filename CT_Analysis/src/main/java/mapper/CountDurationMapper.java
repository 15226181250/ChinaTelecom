package mapper;

import define_type.key.CombineDimension;
import define_type.key.ContactDimension;
import define_type.key.DateDimension;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.Text;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author longlong
 * @create 2020 02 15 1:53
 */
public class CountDurationMapper extends TableMapper<CombineDimension, Text> {

    private CombineDimension combineDimension = new CombineDimension();
    private Map<String,String> phoneNameMap = new HashMap<String, String>();;
    private Text durationText = new Text();

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        //super.setup(context);
        //偷个懒，实际生产是在Hbase中建张表，从表中一条一条读
        phoneNameMap.put("15369468720", "李雁");
        phoneNameMap.put("19920860202", "卫艺");
        phoneNameMap.put("18411925860", "仰莉");
        phoneNameMap.put("14473548449", "陶欣悦");
        phoneNameMap.put("18749966182", "施梅梅");
        phoneNameMap.put("19379884788", "金虹霖");
        phoneNameMap.put("19335715448", "魏明艳");
        phoneNameMap.put("18503558939", "华贞");
        phoneNameMap.put("13407209608", "华啟倩");
        phoneNameMap.put("15596505995", "仲采绿");
        phoneNameMap.put("17519874292", "卫丹");
        phoneNameMap.put("15178485516", "戚丽红");
        phoneNameMap.put("19877232369", "何翠柔");
        phoneNameMap.put("18706287692", "钱溶艳");
        phoneNameMap.put("18944239644", "钱琳");
        phoneNameMap.put("17325302007", "缪静欣");
        phoneNameMap.put("18839074540", "焦秋菊");
        phoneNameMap.put("19879419704", "吕访琴");
        phoneNameMap.put("16480981069", "沈丹");
        phoneNameMap.put("18674257265", "褚美丽");
        phoneNameMap.put("18302820904", "孙怡");
        phoneNameMap.put("15133295266", "许婵");
        phoneNameMap.put("17868457605", "曹红恋");
        phoneNameMap.put("15490732767", "吕柔");
    }

    @Override
    protected void map(ImmutableBytesWritable key, Result value, Context context) throws IOException, InterruptedException {
        //错误代码：String[] words = key.toString().split("_");
        //错误代码：if (words[4].equals("0")) return;
        String rowKey = Bytes.toString(value.getRow());
        String[] words = rowKey.split("_");
        String flag = words[4];
        //只拿到主叫数据即可
        if(StringUtils.equals(flag, "0")) return;
        //以下数据均为主叫数据，但也包含被叫电话的数据
        String caller = words[1];
        String callee = words[3];
        String buildTime = words[2];
        String duration = words[5];
        durationText.set(duration);

        String year = buildTime.substring(0,4);
        String mouth = buildTime.substring(4,6);
        String day = buildTime.substring(6,8);

        //组装CombineDimension
        //组装
        DateDimension yearDimension = new DateDimension(year,"-1","-1");
        DateDimension monthDimension = new DateDimension(year,mouth,"-1");
        DateDimension dayDimension = new DateDimension(year,mouth,day);
        //组装
        ContactDimension callerContactDimension = new ContactDimension(caller,phoneNameMap.get(caller));

        //开始聚合主叫数据
        combineDimension.setContactDimension(callerContactDimension);
        //按年查
        combineDimension.setDateDimension(yearDimension);
        context.write(combineDimension,durationText);
        //按月查
        combineDimension.setDateDimension(monthDimension);
        context.write(combineDimension,durationText);
        //按天查
        combineDimension.setDateDimension(dayDimension);
        context.write(combineDimension,durationText);

        ContactDimension calleeContactDimension = new ContactDimension(callee,phoneNameMap.get(callee));

        //开始聚合被叫数据
        combineDimension.setContactDimension(calleeContactDimension);
        //按年查
        combineDimension.setDateDimension(yearDimension);
        context.write(combineDimension,durationText);
        //按月查
        combineDimension.setDateDimension(monthDimension);
        context.write(combineDimension,durationText);
        //按天查
        combineDimension.setDateDimension(dayDimension);
        context.write(combineDimension,durationText);

    }
}
