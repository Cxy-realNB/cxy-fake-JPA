package base.boot;


import base.dbconnection.DataSourceConnection;
import base.util.PackageScan;

/**
 * @author Wonder Chen
 */
public class ApplicationBoot {
    public static void initialize() {
        DataSourceConnection.main(null);
        PackageScan.initialize();
    }
}
