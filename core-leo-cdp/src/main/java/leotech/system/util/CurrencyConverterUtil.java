package leotech.system.util;

public final class CurrencyConverterUtil {
	
	private static final String VND = "VND";
	private static int base_1_usd_to_vnd = 22660;

	private final static double roundAvoid(double value, int places) {
	    double scale = Math.pow(10, places);
	    return Math.round(value * scale) / scale;
	}
	
	public final static double convertVNDtoUSD(double valueInVND) {
		double usd = valueInVND / base_1_usd_to_vnd;
		return roundAvoid(usd, 3);
	}
	
	public final static double convertToUSD(double value, String currencyCode) {
		if(VND.equalsIgnoreCase(currencyCode) ) {
			return convertVNDtoUSD(value);
		}
		return value;		
	}
	
	// TODO should have background job to update the value of base_1_usd_to_vnd

}
