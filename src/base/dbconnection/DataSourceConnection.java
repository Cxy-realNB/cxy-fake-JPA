package base.dbconnection;

import config.DataSourceConstant;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @author Wonder Chen
 */
public class DataSourceConnection {
    private static Connection conn = null;
    private DataSourceConnection(){

    }
    public static Connection getConnection(){
        if (conn == null){
            try{
                Class.forName(DataSourceConstant.DRIVER_CLASS);
                conn = (Connection) DriverManager.getConnection(DataSourceConstant.URL,DataSourceConstant.USER_NAME,DataSourceConstant.PASSWORD);
                System.out.println("Building connection to datasource successful!");
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        return conn;
    }

    public static void closeConnection(){
        if (conn != null){
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }else {
            new Exception("Need to get one connection before close!");
        }
    }

    public static void main(String[] args) {
        getConnection();
    }
}
