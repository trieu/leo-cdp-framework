package test.geolocation;

import com.google.gson.Gson;
import com.google.openlocationcode.OpenLocationCode;

public class TestOpenLocationCode {

	public static void main(String[] args) {
		double latitude = 10.77084159;
		double longitude = 106.627072;
		// TODO Auto-generated method stub
		String locationCode = OpenLocationCode.encode(latitude, longitude);
		System.out.println(locationCode);
		
		
		System.out.println( new Gson().toJson(OpenLocationCode.decode("87P38MJH+88")) );
	}

}
