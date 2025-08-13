package leotech.ssp.adserver.chanel;

import com.atg.openssp.common.core.entry.SessionAgent;
import com.atg.openssp.common.provider.AdProviderReader;
import com.atg.openssp.common.provider.AdProviderWriter;

/**
 * 
 * @author TrieuNT
 *
 */
public class AdProvider implements AdProviderReader, AdProviderWriter {

	private boolean isValid = Boolean.TRUE;

	private static final String currency = "EUR";

	private float cpm;

	private int adid;// sent by adserver

	private String adMarkup;

	private String contentType;

	public AdProvider(boolean isValid, float cpm, int adid, String adMarkup, String contentType) {
		super();
		this.isValid = isValid;
		this.cpm = cpm;
		this.adid = adid;
		this.adMarkup = adMarkup;
		this.contentType = contentType;
	}

	@Override
	public float getPrice() {
		return cpm;
	}

	@Override
	public void setPrice(final float bidPrice) {
		cpm = bidPrice;
	}

	@Override
	public void setIsValid(final boolean valid) {
		isValid = valid;
	}

	@Override
	public boolean isValid() {
		return isValid;
	}

	@Override
	public float getPriceEur() {
		return cpm * 1;
	}

	@Override
	public String getCurrrency() {
		return currency;
	}

	@Override
	public void perform(final SessionAgent agent) {
		// nothing to implement yet
	}

	@Override
	public String buildResponse() {
		return adMarkup;
	}

	@Override
	public String getVendorId() {
		if (adid > 0) {
			return "AdServer_" + adid;
		}
		return null;
	}

	@Override
	public void setPriceEur(final float priceEur) {
		cpm = priceEur;
	}

	@Override
	public String getAdid() {
		return String.valueOf(adid);
	}

	@Override
	public String toString() {
		return "AdservingCampaignProvider [isValid=" + isValid + ", currency=" + currency + ", cpm=" + cpm + ", adid="
				+ adid + ", adMarkup=" + adMarkup + "]";
	}

	@Override
	public String getContentType() {
		return contentType;
	}

}
