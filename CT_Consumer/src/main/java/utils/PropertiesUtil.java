package utils;

import com.sun.org.glassfish.gmbal.Description;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesUtil {

    public static Properties properties;

    static {
        InputStream is = ClassLoader.getSystemResourceAsStream("kafka.properties");
        properties = new Properties();
        try {
            properties.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static String getProperty(String key){
        return properties.getProperty(key);
    }
}
