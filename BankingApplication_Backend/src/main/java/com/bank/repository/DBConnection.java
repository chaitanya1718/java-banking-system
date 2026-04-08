package com.bank.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.bank.util.EnvUtil;

public class DBConnection {

	private static final String DB_HOST = EnvUtil.getOrDefault("BANK_DB_HOST", "localhost");
	private static final String DB_PORT = EnvUtil.getOrDefault("BANK_DB_PORT", "3306");
	private static final String DB_NAME = EnvUtil.getOrDefault("BANK_DB_NAME", "BankingApplication");
	private static final String DB_USER = EnvUtil.getOrDefault("BANK_DB_USER", "root");
	private static final String DB_PASSWORD = EnvUtil.getOrDefault("BANK_DB_PASSWORD", "password");
	
public static Connection getConnection(){
		
		Connection con = null;
		
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			String jdbcUrl = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME;
			con = DriverManager.getConnection(jdbcUrl, DB_USER, DB_PASSWORD);
			
		} catch (ClassNotFoundException | SQLException e) {
			 
			System.out.println(e.getMessage());
		}
		
		return con;
		
	}

}
