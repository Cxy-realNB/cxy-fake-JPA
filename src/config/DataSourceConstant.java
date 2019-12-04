package config;

/**
 * @author Wonder Chen
 */
public class DataSourceConstant {
    public final static String DRIVER_CLASS = "com.mysql.jdbc.Driver";
    public final static String URL = "jdbc:mysql://localhost:3306/sample?useSSL=false";
    public final static String USER_NAME = "root";
    public final static String PASSWORD = "Cxy666#";

    public final static String  SCAN_PATH = System.getProperty("user.dir") + "\\src";
}
