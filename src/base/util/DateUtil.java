package base.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Wonder Chen
 */
public class DateUtil {
    private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static String dateToString(Date date) {
        return dateFormat.format(date);
    }
}
