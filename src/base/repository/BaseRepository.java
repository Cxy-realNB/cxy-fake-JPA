package base.repository;

import base.annotation.SQL;
import com.sun.xml.internal.ws.api.ha.StickyFeature;
import sun.util.resources.cldr.es.TimeZoneNames_es_419;

import java.util.Collection;
import java.util.List;

/**
 * @author Wonder Chen
 */
public interface BaseRepository<T> {
    @SQL(value = "select * from %s")
    public List<T> findAll();

    @SQL(value = "select count(1) from %s")
    public int count();

    @SQL(value = "", sqlType = SQLType.SELECTORINSERT)
    public T save(T obj);

}
