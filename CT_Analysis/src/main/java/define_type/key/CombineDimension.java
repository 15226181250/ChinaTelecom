package define_type.key;

import define_type.base.DimensionBase;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;

/**
 * @author longlong
 * @create 2020 02 15 0:15
 */
public class CombineDimension extends DimensionBase{

    private ContactDimension contactDimension = new ContactDimension();
    private DateDimension dateDimension = new DateDimension();

    public CombineDimension(){
        super();
    }

    public CombineDimension(ContactDimension contactDimension, DateDimension dateDimension) {
        super();
        this.dateDimension = dateDimension;
        this.contactDimension = contactDimension;

    }

    public ContactDimension getContactDimension() {
        return contactDimension;
    }

    public void setContactDimension(ContactDimension contactDimension) {
        this.contactDimension = contactDimension;
    }

    public DateDimension getDateDimension() {
        return dateDimension;
    }

    public void setDateDimension(DateDimension dateDimension) {
        this.dateDimension = dateDimension;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CombineDimension that = (CombineDimension) o;
        return Objects.equals(contactDimension, that.contactDimension) &&
                Objects.equals(dateDimension, that.dateDimension);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contactDimension, dateDimension);
    }

    @Override
    public int compareTo(DimensionBase o) {
        CombineDimension other = (CombineDimension) o;
        int result = this.dateDimension.compareTo(other.dateDimension);
        if (result != 0) return result;
        result = this.contactDimension.compareTo(other.contactDimension);
        //这样比较的最终数序为：年、月、日、人名、电话号码
        return result;
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dateDimension.write(dataOutput);
        contactDimension.write(dataOutput);

    }

    @Override
    public void readFields(DataInput dataInput) throws IOException {
        dateDimension.readFields(dataInput);
        contactDimension.readFields(dataInput);

    }
}
