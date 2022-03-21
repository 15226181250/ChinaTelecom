import java.io.Closeable;
import java.io.IOException;

public interface IConverter {
    // 根据传入的 dimension 对象，获取数据库中对应该对象数据的 id，如果不存在，则插入该数据再返回
    int getDimensionId(BaseDimension dimension) throws IOException;
} 