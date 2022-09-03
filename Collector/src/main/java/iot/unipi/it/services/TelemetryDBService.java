package iot.unipi.it.services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class TelemetryDBService {
	private static final Logger logger = LogManager.getLogger(TelemetryDBService.class);
	
	private final static String DB_DEFAULT_IP = "localhost";
    private final static String DB_DFAULT_PORT = "3306";
    private final static String DB_USER = "root";
    private final static String DB_PASSWORD = "root";
    private final static String DB_NAME = "CGM_telemetry_system";

    private static Connection conn = null;
	private static TelemetryDBService instance = null;

    private TelemetryDBService() {} 
 
    public static TelemetryDBService getInstance() {
        if (instance == null) {
            instance = new TelemetryDBService();
        }
        return instance;
    }
    
    private static void getConnection() {
    	
    	String connStr = "jdbc:mysql://" + DB_DEFAULT_IP + ":" + DB_DFAULT_PORT + "/" + DB_NAME +
    					"?zeroDateTimeBehavior=CONVERT_TO_NULL&serverTimezone=CET";
    	if(conn == null) {
	    	try {
	            // DriverManager: The basic service for managing a set of JDBC drivers.
	            conn = DriverManager.getConnection(connStr,
	                    						DB_USER,
	                    						DB_PASSWORD);
	            //The Driver Manager provides the connection specified in the parameter string
	            if (conn == null) {
	                logger.warn("Connection to Db failed");
	            }
	        } catch (SQLException se) {
	            logger.error("MySQL Connection Failed! ", se);
	            conn = null;
	        }
    	}
		
    }
    
    public boolean cleanDB() {
    	String query =  "DELETE FROM sensors";
    	boolean success = true;
    	getConnection();
    	try (PreparedStatement ps = conn.prepareStatement(query);) 
    	{
    		int insertedRow = ps.executeUpdate();
    		
        } catch (SQLException se) {
        	logger.error("Error in the delete sensor query! ", se);
        	success = false;
        }
    	
		return success;
    }
    
    public boolean addSensor(String nodeId) {
    	String query = "INSERT INTO sensors (nodeId) VALUES (?);";
    	boolean success = true;
    	getConnection();
    	try (PreparedStatement ps = conn.prepareStatement(query);) 
    	{
    		ps.setString(1, nodeId);
    		int insertedRow = ps.executeUpdate();
    		if(insertedRow < 1) {
    			logger.warn("Something wrong during add sensor");
    			success = false;
    		}
    		
        } catch (SQLException se) {
        	logger.error("Error in the insert sensor query! ", se);
        	success = false;
        }
    	
		return success;
    }
    
    public boolean deleteSensor(String nodeId) {
    	String query =  "DELETE FROM sensors where nodeId = ?";
    	boolean success = true;
    	getConnection();
    	try (PreparedStatement ps = conn.prepareStatement(query);) 
    	{
    		ps.setString(1, nodeId);
    		int insertedRow = ps.executeUpdate();
    		if(insertedRow < 1) {
    			logger.warn("Something wrong during add sensor");
    			success = false;
    		}
    		
        } catch (SQLException se) {
        	logger.error("Error in the delete sensor query! ", se);
        	success = false;
        }
        
		return success;
    }
    
    public boolean addObservation(String sensor, int value, long timestamp) {
    	String query = "INSERT INTO observations (sensor, value, timestamp) VALUES (?, ?, ?);";
    	boolean success = true;
    	getConnection();
    	try (PreparedStatement ps = conn.prepareStatement(query);) 
    	{
    		ps.setString(1, sensor);
    		ps.setInt(2, value);
    		ps.setLong(3, timestamp);
    		int insertedRow = ps.executeUpdate();
    		if(insertedRow < 1) {
    			logger.warn("Something wrong during add observation!");
    			success = false;
    		}
    		
        } catch (SQLException se) {
        	logger.error("Error in the add observation query! ", se);
        	success = false;
        }
		return success;
    }
    
    
    public boolean updateSensorState(String sensor, short status) {
    	String query = "UPDATE sensors SET status=? WHERE nodeId=?;";
    	boolean success = true;
    	getConnection();
    	try (PreparedStatement ps = conn.prepareStatement(query);) 
    	{
    		ps.setShort(1, status);
    		ps.setString(2, sensor);
    		int insertedRow = ps.executeUpdate();
    		if(insertedRow < 1) {
    			logger.warn("Something wrong during add observation!");
    			success = false;
    		}
    		
        } catch (SQLException se) {
        	logger.error("Error in the add observation query! ", se);
        	success = false;
        }
		return success;
    }
    
    public boolean checkSensorExistence(String sensor) {
    	String query = "SELECT nodeId FROM sensors WHERE nodeId=?;";
    	boolean success = false;
    	getConnection();
    	try (PreparedStatement ps = conn.prepareStatement(query);) 
    	{
    		ps.setString(1, sensor);
    		ResultSet rs = ps.executeQuery();
    		while(rs.next()) {
    			success = true;
    		}
    		
        } catch (SQLException se) {
        	logger.error("Error in the check sensor existence query! ", se);
        	success = true;
        }
		return success;
    }
    
    public void prova() {
    	getConnection();
    }
}
