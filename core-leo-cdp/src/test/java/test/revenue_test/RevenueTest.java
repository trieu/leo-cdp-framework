package test.revenue_test;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.collections.map.HashedMap;
import test.jdbi.DatabaseManager;
import test.jdbi.Person;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RevenueTest {

	public static void main(String[] args) throws Exception {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("importDate", "2024-08-16 18:17:25.272");
		jsonObject.addProperty("fromDate", "2021-03-16 18:17:25.272");
		jsonObject.addProperty("toDate", "2021-06-16 18:17:25.272");
		jsonObject.addProperty("sellingPoint", "Main Branch");
		jsonObject.addProperty("salesChannel", "Online");
		jsonObject.addProperty("customerCount", 3);
		jsonObject.addProperty("orderCount", 50);
		jsonObject.addProperty("revenue", 12);
		jsonObject.addProperty("importedBy", "John Doe");
		jsonObject.addProperty("image", "image.png");

		System.out.println(jsonObject.toString());

		PostgreSqlDatabaseManager mng = new PostgreSqlDatabaseManager();
		mng.insertRevenue(Revenue.fromJsonObject(jsonObject));
	}
}
