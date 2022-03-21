package reducer;

import define_type.key.CombineDimension;
import define_type.value.CountDuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;

/**
 * @author longlong
 * @create 2020 02 15 2:48
 */
public class CountDurationReducer extends Reducer<CombineDimension, Text,CombineDimension, CountDuration> {
    private CountDuration countDuration = new CountDuration();
    @Override
    protected void reduce(CombineDimension key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        int callSum = 0;
        int callDurationSum = 0;
        for (Text t : values){
            callSum++;
            callDurationSum += Integer.parseInt(t.toString());
        }
        countDuration.setCallSum(String.valueOf(callSum));
        countDuration.setCallDurationSum(String.valueOf(callDurationSum));
        context.write(key,countDuration);
    }
}
