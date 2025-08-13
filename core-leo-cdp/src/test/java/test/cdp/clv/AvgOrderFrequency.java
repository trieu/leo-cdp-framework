package test.cdp.clv;

import java.util.HashMap;
import java.util.Map;

public class AvgOrderFrequency {
    
    public static void main(String[] args) {
        
        // Sample data: customer order history
        Map<Integer, Integer> customerOrderHistory = new HashMap<Integer, Integer>();
        customerOrderHistory.put(1, 3); // Customer 1 made 3 orders
        customerOrderHistory.put(2, 2); // Customer 2 made 2 orders
        customerOrderHistory.put(3, 1); // Customer 3 made 1 order
        
        // Calculate the Average Order Frequency
        double aof = calculateAOF(customerOrderHistory, 30); // AOF for the last 30 days
        System.out.println("Average Order Frequency: " + aof);
    }
    
    public static double calculateAOF(Map<Integer, Integer> customerOrderHistory, int days) {
        
        int totalOrders = 0;
        int uniqueCustomers = customerOrderHistory.size();
        
        // Calculate the total number of orders
        for (Map.Entry<Integer, Integer> entry : customerOrderHistory.entrySet()) {
            totalOrders += entry.getValue();
        }
        
        // Calculate the Average Order Frequency
        double aof = (double) totalOrders / (uniqueCustomers * days);
        
        return aof;
    }
}

