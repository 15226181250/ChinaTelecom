package define_type.value;

import define_type.base.ValueBase;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author longlong
 * @create 2020 02 15 1:19
 */
public class CountDuration extends ValueBase {

    private String callSum;
    private String callDurationSum;

    public CountDuration(){
        super();
    }

    public CountDuration(String callSum, String callDurationSum) {
        this.callSum = callSum;
        this.callDurationSum = callDurationSum;
    }

    public String getCallSum() {
        return callSum;
    }

    public void setCallSum(String callSum) {
        this.callSum = callSum;
    }

    public String getCallDurationSum() {
        return callDurationSum;
    }

    public void setCallDurationSum(String callDurationSum) {
        this.callDurationSum = callDurationSum;
    }

    @Override
    public int compareTo(ValueBase o) {
        return 0;
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeUTF(this.callSum);
        dataOutput.writeUTF(this.callDurationSum);
    }

    @Override
    public void readFields(DataInput dataInput) throws IOException {
        this.callSum =dataInput.readUTF();
        this.callDurationSum =dataInput.readUTF();
    }
}
