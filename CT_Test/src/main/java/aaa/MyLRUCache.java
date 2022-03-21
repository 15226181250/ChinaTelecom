package aaa;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author longlong
 * @create 2020 02 15 20:46
 */
public class MyLRUCache<String,Integer> extends LinkedHashMap<Object,Object>{//设置缓存键类型为String缓存值类型为Integer
    private static final long serialVersionUID = 1L;
    protected int maxElements;

    public MyLRUCache(int maxSize) {
        super(maxSize, 0.75F, true);
        this.maxElements = maxSize;
    }

    protected boolean removeEldestEntry(Map.Entry<Object,Object> eldest) {
        return this.size() > this.maxElements;
    }
}