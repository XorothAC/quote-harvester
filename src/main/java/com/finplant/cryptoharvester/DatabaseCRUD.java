package com.finplant.cryptoharvester;

import java.sql.*;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DatabaseCRUD {
	private static final Logger LOG = LoggerFactory.getLogger(DatabaseCRUD.class);
	
	// JDBC driver name and database URL
	static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";  
	
	// Database credentials
	private static String url;
	private static String user;
	private static String pass;
	private static Connection conn = null;

	public DatabaseCRUD(String url, String user, String pass) {
		DatabaseCRUD.url = "jdbc:mysql://"+url;
		DatabaseCRUD.user = user;
		DatabaseCRUD.pass = pass;
		connectToDB();
	}

	private void connectToDB() {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			
			LOG.info("Connecting to database...");
			conn = DriverManager.getConnection(url,user,pass);
			LOG.info("Connected to database.");
		} catch (Exception e) {
			ErrorHandler.logError("Connection error to database: ", e);
		}
	}
	
	public Connection getConnection() {
		return conn;
	}
	
	public void createQuotesTable() {
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
		
		LOG.info("Attempting to create QUOTES table in database...");
		executeSQL(sql);
	}
	
	public void quoteBatchStatement(List<Quote> quotes) {
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			for (Quote quote : quotes) {
				stmt.addBatch("INSERT INTO QUOTES(TIME, BID, ASK, EXCHANGE, NAME) "
						+ "VALUES (" + "now()" + ", " + 
								quote.getBid().toString() + ", " + 
								quote.getAsk().toString() + ", '" + 
								quote.getExchange() + "', '" + 
								quote.getName() + "')");
			}
			stmt.executeBatch();
		} catch (SQLException e) {
			try {
				stmt.close();
				conn.close();
			} catch (Exception e1) {
				ErrorHandler.logError("Statement closing error: ", e1);
			}
			
			try {
				conn.close();
			} catch (Exception e1) {
				ErrorHandler.logError("Connection closing error: ", e1);
			}
			ErrorHandler.logError("SQL syntax error: ", e);
		}
		
		quotes.clear();
	}

	public static void executeSQL(String sql) {
		Statement stmt = null;
	
		try {
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
		} catch (SQLException e) {
			try {
				stmt.close();
				conn.close();
			} catch (Exception e1) {
				ErrorHandler.logError("Statement closing error: ", e1);
			}
			
			try {
				conn.close();
			} catch (Exception e1) {
				ErrorHandler.logError("Connection closing error: ", e1);
			}
			
			ErrorHandler.logError("SQL syntax error: ", e);
		}
	}
}
