package leotech.system.util;

/**
 * 
 * Real-time data processor using Xstream in Redis <br>
 * https://devopedia.org/redis-streams
 * 
 * @author Trieu Nguyen
 * @since 2025
 *
 */
public class RedisXstreamUtil {

	public final static class StreamKeys {
	    public static String profileUpdate(String profileId) {
	        return "leocdp:profile:" + profileId;
	    }

	    public static String eventMetric(String sessionId, String metricName) {
	        return "leocdp:event:" + sessionId + ":" + metricName;
	    }

	    public static String productView(String productId) {
	        return "leocdp:product:" + productId + ":view";
	    }
	    
	    // TODO 
	    // check blog https://www.linkedin.com/pulse/redis-82-brings-powerful-enhancements-streams-meet-xackdel-suyog-kale-figcf
	}
	
	

}
