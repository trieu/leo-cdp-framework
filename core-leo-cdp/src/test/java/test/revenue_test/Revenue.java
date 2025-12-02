package test.revenue_test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.commons.collections.map.HashedMap;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

public class Revenue implements Serializable {
    private String id;
    private Timestamp importDate;
    private Timestamp fromDate;
    private Timestamp toDate;
    private String sellingPoint;
    private String salesChannel;
    private int customerCount;
    private int orderCount;
    private double revenue;
    private String importedBy;
    private String image;

    public Revenue() {}

    public Revenue(String id, Timestamp importDate, Timestamp fromDate, Timestamp toDate ,String sellingPoint, String salesChannel, int customerCount, int orderCount, double revenue, String importedBy, String image) {
        this.id = id;
        this.importDate = importDate;
        this.toDate = toDate;
        this.fromDate = fromDate;
        this.sellingPoint = sellingPoint;
        this.salesChannel = salesChannel;
        this.customerCount = customerCount;
        this.orderCount = orderCount;
        this.revenue = revenue;
        this.importedBy = importedBy;
        this.image = image;
    }

    public String getId() {
        return id;
    }

    public Timestamp getImportDate() {
        return importDate;
    }

    public Timestamp getFromDate() {
        return fromDate;
    }

    public Timestamp getToDate() {
        return toDate;
    }

    public String getSellingPoint() {
        return sellingPoint;
    }

    public String getSalesChannel() {
        return salesChannel;
    }

    public int getCustomerCount() {
        return customerCount;
    }

    public int getOrderCount() {
        return orderCount;
    }

    public double getRevenue() {
        return revenue;
    }

    public String getImportedBy() {
        return importedBy;
    }

    public String getImage() {
        return image;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setImportDate(Timestamp importDate) {
        this.importDate = importDate;
    }

    public void setFromDate(Timestamp fromDate) {
        this.fromDate = fromDate;
    }

    public void setToDate(Timestamp toDate) {
        this.toDate = toDate;
    }

    public void setSellingPoint(String sellingPoint) {
        this.sellingPoint = sellingPoint;
    }

    public void setSalesChannel(String salesChannel) {
        this.salesChannel = salesChannel;
    }

    public void setCustomerCount(int customerCount) {
        this.customerCount = customerCount;
    }

    public void setOrderCount(int orderCount) {
        this.orderCount = orderCount;
    }

    public void setRevenue(double revenue) {
        this.revenue = revenue;
    }

    public void setImportedBy(String importedBy) {
        this.importedBy = importedBy;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public PreparedStatement getPreparedStatement(String sql, Connection connection) {
        try {
            PreparedStatement pstmt = connection.prepareStatement(sql);

            if(this.id == null || this.id.isBlank()) {
                this.id = UUID.randomUUID().toString();
            }

            pstmt.setString(1, this.id);
            pstmt.setTimestamp(2, this.importDate);
            pstmt.setTimestamp(3, this.fromDate);
            pstmt.setTimestamp(4, this.toDate);
            pstmt.setString(5, this.sellingPoint);
            pstmt.setString(6, this.salesChannel);
            pstmt.setInt(7, this.customerCount);
            pstmt.setInt(8, this.orderCount);
            pstmt.setDouble(9, this.revenue);
            pstmt.setString(10, this.importedBy);
            pstmt.setString(11, this.image);

            return pstmt;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Map<String, Object> validateRevenue(Connection connection) {
        boolean isValid = true;
        String reason = "";
        @SuppressWarnings("unchecked")
        Map<String, Object> map = new HashedMap();
        String sql = "SELECT * FROM cdp_revenue.revenue WHERE from_date >= ? AND to_date <= ?";

        try {
            // make sure all fields can not be null, String fields can not be blank and Number fields can not be 0
            Field[] fields = this.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                if (field.get(this) == null ||
                        (field.getType().equals(String.class) && field.get(this).toString().isBlank())) {
                    map.put("isValid", false);
                    map.put("reason", field.getName() + " is null");

                    return map;
                }
                else if(field.getType().equals(int.class)) {
                    int num = field.getInt(this);

                    if(num <= 0) {
                        map.put("isValid", false);
                        map.put("reason", field.getName() + " must be higher than 0");

                        return map;
                    }
                } else if (field.getType().equals(double.class)) {
                    double num = field.getDouble(this);

                    if(num <= 0) {
                        map.put("isValid", false);
                        map.put("reason", field.getName() + " must be higher than 0");

                        return map;
                    }
                }
            }

            // check if importDate is after both fromDate and toDate or not
            if(this.importDate.compareTo(this.fromDate) < 0 || this.importDate.compareTo(this.toDate) < 0) {
                isValid = false;
                reason = "Thời gian nhập phải sau 2 ngày bắt đầu và kết thúc tính doanh thu";
            }
            // check if fromDate is before or equal to toDate or not
            else if(this.fromDate.compareTo(this.toDate) > 0) {
                isValid = false;
                reason = "Thời gian bắt đầu tính doanh thu phải trước thời gian kết thúc tính doanh thu";
            }
            // check if there is any data at period between fromDate and toDate
            else {
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setTimestamp(1, this.fromDate);
                preparedStatement.setTimestamp(2, this.toDate);

                ResultSet rs = preparedStatement.executeQuery();

                if(rs.next()) {
                    isValid = false;
                    reason = "Khoảng thời gian bắt đầu tính doanh thu và thời gian kết thúc tính doanh thu đã tồn tại";
                }
            }

            map.put("isValid", isValid);
            map.put("reason", reason);

            return map;
        }
        catch (Exception e) {
            e.printStackTrace();

            map.put("isValid", false);
            map.put("reason", e.getMessage());

            return map;
        }
    }

    @Override
    public String toString() {
        return new GsonBuilder()
                .registerTypeAdapter(Timestamp.class, new TimestampDeserializer())
                .create()
                .toJson(this);
    }

    public static Revenue fromJsonObject(JsonObject jsonObject) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Timestamp.class, new TimestampDeserializer())
                .create();
        return gson.fromJson(jsonObject, Revenue.class);
    }
}
