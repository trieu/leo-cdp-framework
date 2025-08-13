package test.util;

import java.util.Arrays;
import java.util.List;

import leotech.system.util.RoundRobin;

public class TestRoundRobin {
    public static void main(String[] args) {    	
    	String str = "A, B, C, D";
    	List<String> items = Arrays.asList(str.split("\\s*,\\s*"));
    	RoundRobin<String> roundRobin = new RoundRobin<>(items);
    	
        for (int i = 0; i < 10; i++) {
            System.out.println(roundRobin.next());
        }
    }
}
