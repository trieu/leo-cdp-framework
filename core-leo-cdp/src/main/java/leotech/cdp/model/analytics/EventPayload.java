package leotech.cdp.model.analytics;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import com.google.gson.annotations.SerializedName;

public class EventPayload {

	// Map JSON keys to Java fields
	@SerializedName("obsid")
	private String obsId;

	@SerializedName("mediahost")
	private String mediaHost;

	@SerializedName("tprefurl")
	private String tpRefUrl;

	@SerializedName("tprefdomain")
	private String tpRefDomain;

	@SerializedName("tpurl")
	private String tpUrl;

	@SerializedName("tpname")
	private String tpName;

	@SerializedName("metric")
	private String metric;

	@SerializedName("eventdata")
	private String eventData;

	@SerializedName("visid")
	private String visId;

	@SerializedName("fgp")
	private String fgp;
	

	// --- Getters ---

	public String getObsId() {
		return obsId;
	}

	public String getMediaHost() {
		return mediaHost;
	}

	public String getMetric() {
		return metric;
	}

	public String getVisId() {
		return visId;
	}

	public String getTpRefUrl() {
		return tpRefUrl;
	}

	public String getTpRefDomain() {
		return tpRefDomain;
	}

	public String getTpUrl() {
		return tpUrl;
	}

	public String getTpName() {
		return tpName;
	}

	public String getEventData() {
		return eventData;
	}

	public String getFgp() {
		return fgp;
	}

	/**
	 * Helper: Gets the raw URL-encoded string from JSON
	 */
	public String getRawEventData() {
		return eventData;
	}

	/**
	 * Helper: Decodes the double-encoded string often sent by JS SDKs. Example:
	 * "%7B%22stockSymbol%22..." -> "{"stockSymbol"..."
	 */
	public String getDecodedEventData() {
		return decode(eventData);
	}

	public String getDecodedTpUrl() {
		return decode(tpUrl);
	}

	public String getDecodedTpName() {
		return decode(tpName);
	}

	// Utility to handle URL decoding (handles double encoding if present)
	private String decode(String value) {
		if (value == null)
			return null;
		try {
			// First decode
			String decoded = URLDecoder.decode(value, StandardCharsets.UTF_8.name());
			// Check if it needs a second pass (common in complex JS SDKs)
			if (decoded.contains("%")) {
				return URLDecoder.decode(decoded, StandardCharsets.UTF_8.name());
			}
			return decoded;
		} catch (UnsupportedEncodingException e) {
			return value;
		}
	}

	@Override
	public String toString() {
		return "Event: " + metric + " | Data: " + getDecodedEventData();
	}
}
