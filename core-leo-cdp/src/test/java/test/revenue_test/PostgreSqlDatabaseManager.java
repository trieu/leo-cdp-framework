package test.revenue_test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Map;

public class PostgreSqlDatabaseManager {
    private static final String URL = "jdbc:postgresql://180.93.182.219:5432/cdp_everon";
    private static final String USER = "postgre";
    private static final String PASSWORD = "postgre ";

    private final Connection connection;

    public PostgreSqlDatabaseManager() throws Exception {
        this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public void insertRevenue(Revenue revenue) {
        String sql = "INSERT INTO cdp_revenue.revenue (id, import_date, from_date, to_date, selling_point, sales_channel, customer_count, order_count, revenue, imported_by, image) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            PreparedStatement preparedStatement = revenue.getPreparedStatement(sql, connection);

            Map<String, Object> resMap = revenue.validateRevenue(connection);

            if((boolean) resMap.get("isValid")) {
                int successfulRow = preparedStatement.executeUpdate();
                System.out.println("Inserted " + successfulRow + " to database successfully");
            }
            else {
                System.out.println(resMap.get("reason").toString());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("Inserted new revenue to database failed");
        }
    }
}
