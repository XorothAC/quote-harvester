package com.finplant.cryptoharvester;

import java.sql.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DatabaseCRUD {
	private static final Logger LOG = LoggerFactory.getLogger(DatabaseCRUD.class);
	
	// JDBC driver name and database URL
	static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";  
	
	//  Database credentials
	private static String url;
	private static String user;
	private static String pass;

	public DatabaseCRUD(String url, String user, String pass) {
		DatabaseCRUD.url = "jdbc:mysql://"+url+"/sys";
		DatabaseCRUD.user = user;
		DatabaseCRUD.pass = pass;
	}
	
	public static void connectDB() {
		Connection conn = null;
		Statement stmt = null;
		
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			
			LOG.info("Connecting to database...");
			conn = DriverManager.getConnection(url,user,pass);
			LOG.info("Connected to database.");
			
			stmt = conn.createStatement();
			
			String sql = "CREATE TABLE IF NOT EXISTS QUOTES (" +
	                   "ID int(11) NOT NULL AUTO_INCREMENT, " +
	                   "TIME TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " + 
	                   "BID decimal(20,8) NOT NULL, " + 
	                   "ASK decimal(20,8) NOT NULL, " + 
	                   "EXCHANGE varchar(20) NOT NULL, " +
	                   "NAME varchar(20) NOT NULL, " +
	                   "PRIMARY KEY (ID), " +
	                   "KEY QUOTES_TIME_IDX (TIME) USING BTREE, " +
	                   "KEY QUOTES_SYNTHETIC_IDX (NAME) USING BTREE" +
	                   ") ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8";
			
			LOG.info("Creating QUOTES table in database...");
			stmt.executeUpdate(sql);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if(conn!=null) conn.close();
				LOG.info("Connection closed.");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
