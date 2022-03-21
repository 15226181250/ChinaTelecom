package define_type.key;

import define_type.base.DimensionBase;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;

/**
 * @author longlong
 * @create 2020 02 14 23:54
 */
public class ContactDimension extends DimensionBase {
    private String phone;
    private String name;

    public ContactDimension(){super();};
    public ContactDimension(String phone, String name) {
        super();
        this.phone = phone;
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContactDimension that = (ContactDimension) o;
        return Objects.equals(phone, that.phone) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(phone, name);
    }

    @Override
    public int compareTo(DimensionBase o) {
        ContactDimension other = (ContactDimension) o;
        int result = this.name.compareTo(other.name);
        //结果为0说明一样，没比较出结果
        if (result != 0) return result;
        result = this.phone.compareTo(other.phone);
        return result;
    }

    @Override
    public void write(DataOutput dataOutput) throws IOException {
        dataOutput.writeUTF(this.phone);
        dataOutput.writeUTF(this.name);
    }

    @Override
    public void readFields(DataInput dataInput) throws IOException {
        this.phone = dataInput.readUTF();
        this.name = dataInput.readUTF();
    }
}
