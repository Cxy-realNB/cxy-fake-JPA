package base.proxy;

import base.annotation.Column;
import base.annotation.Entity;
import base.annotation.Key;
import base.annotation.Param;
import base.annotation.SQL;
import base.cache.RunTimeCache;
import base.dbconnection.DataSourceConnection;
import base.exception.CxyJPAException;
import base.repository.BaseRepository;
import base.repository.SQLType;
import base.util.DateUtil;
import com.sun.org.apache.bcel.internal.generic.RETURN;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Wonder Chen
 */
public class Executor implements InvocationHandler {

    private String executorName;
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        SQL sql = method.getAnnotation(SQL.class);
        String sqlString = sql.value();
        switch (sql.sqlType()) {
            case DELETE:
            case INSERT:
            case UPDATE:
                return executeDML(sqlString, method, args);
            case SELECT:
                return executeSelect(sqlString, method, args);
            case SELECTORINSERT:
                return executeSave(args);
            default:

        }
        return null;
    }

    private <T> T executeDML(String sql, Method method, Object[] args) {
        Annotation[][] params = method.getParameterAnnotations();
        for (int i = 0; i < params.length; i++) {
            Param param = (Param) params[i][0];
            sql = args[i] instanceof String ? sql.replace(":" + param.value(), "'" + args[i] + "'") : sql.replace(":" + param.value(), String.valueOf(args[i]));
        }
        Integer result = 0;
        try {
            PreparedStatement preparedStatement = DataSourceConnection.getConnection().prepareStatement(sql);
            result = preparedStatement.executeUpdate();
            System.out.println(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return (T) result;
    }

    private <T> T executeSelect(String sql, Method method, Object[] args){
        Annotation[][] params = method.getParameterAnnotations();
        if (method.getDeclaringClass().getTypeName().equals("base.repository.BaseRepository")) {
            try {
                Class clazz = Class.forName(RunTimeCache.ENTITY_CACHE.get(RunTimeCache.REPOSITORY_GENERICTYPE_CACHE.get(executorName)));
                Entity entity = (Entity) clazz.getAnnotation(Entity.class);
                if (entity.tableName().isEmpty()){
                    throw new CxyJPAException("tableName can not be null!");
                }else {
                    sql = String.format(sql, entity.tableName());
                }
            } catch (ClassNotFoundException | CxyJPAException e) {
                e.printStackTrace();
            }
        }
        return dealDQL(method, args, sql, params);
    }

    private <T> T executeSave(Object[] args) {
        Object instance = args[0];
        Class clazz = instance.getClass();
        List<Field> keyList = Arrays.asList(clazz.getDeclaredFields()).stream().filter(x -> x.getAnnotation(Key.class) != null).collect(Collectors.toList());
        Field keyField = keyList.get(0);
        keyField.setAccessible(true);
        String tableName = ((Entity) clazz.getAnnotation(Entity.class)).tableName();
        String columnName = keyField.getAnnotation(Column.class).name();
        switch (keyField.getType().getTypeName()){
            case "java.lang.Integer":
            case "long":
            case "java.lang.Long":
            case "int":
                String findMaxId = String.format("select %s from %s order by %s desc", columnName, tableName, columnName);
                try {
                    PreparedStatement preparedStatement = DataSourceConnection.getConnection().prepareStatement(findMaxId, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                    Long keyValue = (Long) keyField.get(instance);
                    if (keyValue == 0){
                        Integer idMax = null;
                        ResultSet resultSet = preparedStatement.executeQuery();
                        while (resultSet.next()) {
                            idMax = resultSet.getInt(1);
                            resultSet.updateRow();
                            break;
                        }
                        keyField.set(instance, idMax + 1);
                        preparedStatement = DataSourceConnection.getConnection().prepareStatement(joinInsertSQL(instance, tableName));
                        Integer result = preparedStatement.executeUpdate();
                        return (T) result;
                    }else {
                        Integer result = null;
                        String ifHave = String.format("select * from %s where %s = %s", tableName, columnName, keyValue);
                        PreparedStatement preparedStatementJudge = DataSourceConnection.getConnection().prepareStatement(ifHave, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                        ResultSet resultSet = preparedStatementJudge.executeQuery();
                        if (resultSet.next() == true){
                            if (resultSet.next() == false){
                                resultSet.previous();
                                Arrays.asList(clazz.getDeclaredFields()).stream()
                                        .filter(x -> x.getAnnotation(Column.class) != null)
                                        .collect(Collectors.toList()).stream()
                                        .forEach(x -> {
                                            try {
                                                x.setAccessible(true);
                                                resultSet.updateObject(x.getAnnotation(Column.class).name(), x.get(instance));
                                                resultSet.updateRow();
                                            } catch (IllegalAccessException | SQLException e) {
                                                e.printStackTrace();
                                            }
                                        });
                            }else {
                                throw new CxyJPAException("one more data with same primary key is invalid! invalid key :" + keyField.getName());
                            }
                        }else {
                            PreparedStatement  preparedStatement2 = DataSourceConnection.getConnection().prepareStatement(joinInsertSQL(instance, tableName));
                            result = preparedStatement2.executeUpdate();
                            return (T) result;
                        }
                    }
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (!DataSourceConnection.getConnection().getAutoCommit()) {
                            DataSourceConnection.getConnection().commit();
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            default:
        }
        return null;
    }


    private String joinInsertSQL(Object obj, String tableName) {
        String insert = "insert into %s ( %s ) values ( %s )";
        StringBuilder column = new StringBuilder();
        StringBuilder values = new StringBuilder();
        Class clazz = obj.getClass();
        Arrays.asList(clazz.getDeclaredFields()).stream()
                .filter(x -> x.getAnnotation(Column.class) != null)
                .collect(Collectors.toList()).stream()
                .forEach(x -> {
                    x.setAccessible(true);
                    column.append(x.getAnnotation(Column.class).name());
                    column.append(",");
                    try {
                        switch (x.getType().getTypeName()){
                            case "java.lang.String":
                                values.append("'");
                                values.append(x.get(obj));
                                values.append("'");
                                break;
                            case "java.util.Date":
                                values.append("'");
                                values.append(DateUtil.dateToString((Date) x.get(obj)));
                                values.append("'");
                                break;
                            default:
                                values.append(x.get(obj));
                        }
                        values.append(",");
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                });
        String sql = String.format(insert, tableName, column.substring(0, column.length() - 1), values.substring(0, values.length() - 1));
        System.out.println(sql);
        return sql;
    }

    private <T> T dealDQL(Method method, Object[] args, String sqlString, Annotation[][] params) {
        for (int i = 0; i < params.length; i++) {
            Param param = (Param) params[i][0];
            sqlString = args[i] instanceof String ? sqlString.replace(":" + param.value(), "'" + args[i] + "'") : sqlString.replace(":" + param.value(), String.valueOf(args[i]));
        }
        Type type = method.getGenericReturnType();
        if (type instanceof ParameterizedType){
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            return analyzeTypeAndExecuteDQL(sqlString, parameterizedType.getRawType(), actualTypeArguments[0]);
        }else {
            return analyzeTypeAndExecuteDQL(sqlString, type);
        }
    }

    private <T> T analyzeTypeAndExecuteDQL(String sql, Type... types) {
        T result = null;
        try {
            PreparedStatement preparedStatement = DataSourceConnection.getConnection().prepareStatement(sql);
            ResultSet resultSet = preparedStatement.executeQuery();
            System.out.println(sql);
            System.out.println(types[0].getTypeName());
            if (types.length == 2){
                if ("java.util.List".equals(types[0].getTypeName())){
                    result = listExecutor(resultSet, types[1]);
                }else {
                    throw new CxyJPAException("you should use designated returnType!");
                }
            }else {
                switch (types[0].getTypeName()) {
                    case "java.util.List":
                        result = listExecutor(resultSet);
                        break;
                    case "java.lang.String":
                        result = stringExecutor(resultSet);
                        break;
                    case "java.lang.Integer":
                    case "long":
                    case "java.lang.Long":
                    case "int":
                        result = numberExecutor(resultSet);
                        break;
                    case "java.lang.Boolean":
                    case "boolean":
                        result = booleanExecutor(resultSet);
                        break;
                    default:
                        throw new CxyJPAException("you should use designated returnType!");
                }
            }
        } catch (SQLException | CxyJPAException e) {
            e.printStackTrace();
        }
        return result;
    }

    private <T> T listExecutor(ResultSet resultSet, Type type) {
        List result = new ArrayList();
        try {
            Class clazz = Class.forName(RunTimeCache.ENTITY_CACHE.get(type.getTypeName()));
            Field[] fields = clazz.getDeclaredFields();
            while (resultSet.next()){
                Object element = clazz.newInstance();
                for (Field f : fields){
                    Column column = f.getAnnotation(Column.class);
                    if (column != null){
                        f.setAccessible(true);
                        System.out.println(f.getType().getTypeName());
                        switch(f.getType().getTypeName()){
                            case "java.lang.String":
                                f.set(element, resultSet.getString(column.name()));
                                break;
                            case "java.lang.Integer":
                            case "long":
                            case "java.lang.Long":
                            case "int":
                                f.set(element, resultSet.getInt(column.name()));
                                break;
                            case "java.lang.Boolean":
                            case "boolean":
                                f.set(element, resultSet.getBoolean(column.name()));
                                break;
                            case "java.util.Date":
                                f.set(element, resultSet.getDate(column.name()));
                                break;
                            default:
                        }
                    }
                }
                result.add(element);
            }
        } catch (SQLException | IllegalAccessException | ClassNotFoundException | InstantiationException e) {
            e.printStackTrace();
        }
        return (T) result;
    }

    private <T> T listExecutor(ResultSet resultSet) {
        List result = new ArrayList();
        try {
            int columnCount = resultSet.getMetaData().getColumnCount();
            while (resultSet.next()){
                List element = new ArrayList();
                for (int i = 1 ;i <= columnCount; i++){
                    element.add(resultSet.getString(i));
                }
                result.add(element);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return (T) result;
    }

    private <T> T stringExecutor(ResultSet resultSet) {
        String result = null;
        try {
            while (resultSet.next()){
                result = resultSet.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return (T) result;
    }

    private <T> T numberExecutor(ResultSet resultSet) {
        Integer result = null;
        try {
            while (resultSet.next()){
                result = resultSet.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return (T) result;
    }
    private <T> T booleanExecutor(ResultSet resultSet) {
        Boolean result = null;
        try {
            while (resultSet.next()){
                result = resultSet.getBoolean(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return (T) result;
    }

    public static <T> T getHandle (Class<? extends BaseRepository> mapperInterface){
        if (!RunTimeCache.REPOSITORY_HANDLE_CACHE.containsKey(mapperInterface.getTypeName())) {
            Executor executor = new Executor();
            executor.setExecutorName(mapperInterface.getTypeName());
            RunTimeCache.REPOSITORY_HANDLE_CACHE.put(mapperInterface.getTypeName(), Proxy.newProxyInstance(mapperInterface.getClassLoader(), new Class[]{mapperInterface}, executor));
        }
        if (!RunTimeCache.REPOSITORY_GENERICTYPE_CACHE.containsKey(mapperInterface.getTypeName())) {
            Type[] genericInterfaces = mapperInterface.getGenericInterfaces();
            Type genericClass = ((ParameterizedType) genericInterfaces[0]).getActualTypeArguments()[0];
            RunTimeCache.REPOSITORY_GENERICTYPE_CACHE.put(mapperInterface.getTypeName(), genericClass.getTypeName());
        }
        return (T) RunTimeCache.REPOSITORY_HANDLE_CACHE.get(mapperInterface.getTypeName());
    }

    public void setExecutorName(String executorName) {
        this.executorName = executorName;
    }
}
