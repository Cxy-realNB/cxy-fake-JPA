package base.cache;

import com.mysql.jdbc.log.NullLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Wonder Chen
 */
public class RunTimeCache {
    public static final Map<String,String> ENTITY_CACHE = new HashMap<>();
    public static final Map<String,Object> REPOSITORY_HANDLE_CACHE = new ConcurrentHashMap<>();
    public static final Map<String,String> REPOSITORY_GENERICTYPE_CACHE = new ConcurrentHashMap<>();
}
