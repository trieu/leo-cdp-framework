package test.sql;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import leotech.cdp.domain.processor.SqlDatabaseImportProcessor;
import rfx.core.util.Utils;

public class TestPostgresSql {
	
	public static void main(String[] args) {	
		Map<String, String> fieldMap = ImmutableMap.<String, String> builder()			      
			      .put("firstName","name")
			      .put("gender", "gender")
			      .put("dateOfBirth", "birthday")
			      .put("primaryPhone","phone")
			      .put("primaryEmail", "email")
			      .put("notes", "notes")
			      .put("saleAgents", "salesman")
			      .put("saleAgencies", "customersource")
			      .put("dataLabels", "project")			      
			      .build();
		
		String dataTable = "sample_data";
		String dbUrl = "jdbc:postgresql://localhost:2345/test";
		String username = "postgres";
		String password = "123456";
		int batchSize = 50, maxResult = 200;
		String sqlTemplate = "SELECT * FROM $dataTable ORDER BY createdate DESC LIMIT $limit OFFSET $offset ";
		
		new SqlDatabaseImportProcessor(sqlTemplate, fieldMap).importProfileFromSqlDatabase(dataTable, dbUrl, username, password, batchSize, maxResult);
		
		Utils.exitSystemAfterTimeout(30000);
	}
}
