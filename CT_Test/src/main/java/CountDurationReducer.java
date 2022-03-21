
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;

public class CountDurationReducer extends Reducer<ComDimension, Text, ComDimension,
         CountDurationValue>{
@Override
protected void reduce(ComDimension key, Iterable<Text> values, Context context) throws
        IOException, InterruptedException {

        int count = 0;
        int sumDuration = 0;
        for(Text text : values){
        count ++;
        sumDuration += Integer.valueOf(text.toString());
        }
        CountDurationValue countDurationValue = new CountDurationValue(count,
        sumDuration);
        context.write(key, countDurationValue);
        }
        }