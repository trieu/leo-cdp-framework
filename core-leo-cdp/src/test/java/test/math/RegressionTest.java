package test.math;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.math3.stat.regression.RegressionResults;
import org.apache.commons.math3.stat.regression.SimpleRegression;

public class RegressionTest {
	

	protected static void testRegression() {
		SimpleRegression simpleRegression = new SimpleRegression();

        ArrayList<Double> timeSeries = new ArrayList<Double>(Arrays.asList(3.0, 5.0, 1.0, 7.0, 9.0, 2.0, 1.0, 6.0, 8.0));

        for(int i = 0; i < timeSeries.size(); i++) {
            simpleRegression.addData(i, timeSeries.get(i));
        }
        RegressionResults rs = simpleRegression.regress();
        System.out.println(rs.getN());
       

        System.out.println("Start date unit at t = 0:");
        System.out.println("Intercept: " + simpleRegression.getIntercept());
        System.out.println("Slope    : " + simpleRegression.getSlope());
        System.out.println("predict 5    : " + simpleRegression.predict(5));
        System.out.println("predict 9    : " + simpleRegression.predict(9));
        System.out.println("predict 16    : " + simpleRegression.predict(16));
        System.out.println("predict 26    : " + simpleRegression.predict(26));
        System.out.println("predict 28    : " + simpleRegression.predict(28));
        System.out.println("predict 66    : " + simpleRegression.predict(66));
	}
	
	public static void main(String[] args) {

		String s = "The answer to life, the universe, and everything: ";
        
		BigInteger a = new BigInteger("-80538738812075974");
        BigInteger b = new BigInteger("80435758145817515");
        BigInteger c = new BigInteger("12602123297335631");
        //compute 42
        BigInteger m = a.pow(3).add(b.pow(3)).add(c.pow(3));
        
        double a2 = -80538738812075974L;
        double b2 = 80435758145817515L;
        double c2 = 12602123297335631L;
        //compute 42
        double n = Math.pow(a2,3) + Math.pow(b2,3) + Math.pow(c2,3);
        
        
        System.out.println(s+ m);
        System.out.println(s + n);

    }
}
