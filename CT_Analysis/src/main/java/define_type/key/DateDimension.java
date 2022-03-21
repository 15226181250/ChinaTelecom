package define_type.key;

import define_type.base.DimensionBase;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;

/**
 * @author longlong
 * @create 2020 02 15 0:05
 */
public class DateDimension extends DimensionBase {

    private String year;
    private String mouth;
    private String day;

    public DateDimension(){
        super();
    }

    public DateDimension(String year,String mouth,String day){
        super();
        this.year = year;
        this.mouth = mouth;
        this.day = day;
    }


    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getMouth() {
        return mouth;
    }

    public void setMouth(String mouth) {
        this.mouth = mouth;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DateDimension that = (DateDimension) o;
        return Objects.equals(year, that.year) &&
                Objects.equals(mouth, that.mouth) &&
                Objects.equals(day, that.day);
    }

    @Override
    public int hashCode() {
        return Objects.hash(year, mouth, day);
    }

    @Override
    public int compareTo(DimensionBase o) {
        DateDimension other = (DateDimension)o;
        int result = this.year.compareTo(other.year);
        if (result != 0) return result;
        result = this.mouth.compareTo(other.mouth);
        if (result != 0) return result;
        result = this.day.compareTo(other.day);
        return result;
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeUTF(this.year);
        dataOutput.writeUTF(this.mouth);
        dataOutput.writeUTF(this.day);
    }

    @Override
    public void readFields(DataInput dataInput) throws IOException {
        this.year = dataInput.readUTF();
        this.mouth = dataInput.readUTF();
        this.day = dataInput.readUTF();
    }
}
