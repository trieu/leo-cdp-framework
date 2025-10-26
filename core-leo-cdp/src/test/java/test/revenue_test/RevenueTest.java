package test.revenue_test;

import com.google.gson.JsonObject;

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
