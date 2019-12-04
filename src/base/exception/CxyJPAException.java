package base.exception;

import base.dbconnection.DataSourceConnection;

/**
 * @author Wonder Chen
 */
public class CxyJPAException extends Exception {

    public CxyJPAException(String message){
        super(message);
    }

    @Override
    public void printStackTrace(){
        super.printStackTrace();
        mandatoryCloseDBConnection();
    }


    public void mandatoryCloseDBConnection(){
        DataSourceConnection.closeConnection();
    }

}
