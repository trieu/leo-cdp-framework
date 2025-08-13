package leotech.cdp.model.analytics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;

/**
 * The 5As Customer Framework <br> <br>
 * https://knowledge.leocdp.net/2022/05/the-5as-customer-framework.html <br>
 * https://datahub4uspa.leocdp.net/content/1w0bTjf2RNnhkm6hFZZ7HS-marketing-4-0-phan-3-mo-hinh-5a-trong-thoi-ai-so
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public final class JourneyReport implements DataJourneyStatistics{

	int visitorCountAwareness = 0, visitorCountAttraction = 0, visitorCountAsk = 0, visitorCountAction = 0,
			visitorCountAdvocacy = 0;
	int contactCountAwareness = 0, contactCountAttraction = 0, contactCountAsk = 0, contactCountAction = 0,
			contactCountAdvocacy = 0;

	public JourneyReport() {

	}

	public int getVisitorCountAwareness() {
		return visitorCountAwareness;
	}

	public void setVisitorCountAwareness(int visitorCountAwareness) {
		this.visitorCountAwareness = visitorCountAwareness;
	}

	public int getVisitorCountAttraction() {
		return visitorCountAttraction;
	}

	public void setVisitorCountAttraction(int visitorCountAttraction) {
		this.visitorCountAttraction = visitorCountAttraction;
	}

	public int getVisitorCountAsk() {
		return visitorCountAsk;
	}

	public void setVisitorCountAsk(int visitorCountAsk) {
		this.visitorCountAsk = visitorCountAsk;
	}

	public int getVisitorCountAction() {
		return visitorCountAction;
	}

	public void setVisitorCountAction(int visitorCountAction) {
		this.visitorCountAction = visitorCountAction;
	}

	public int getVisitorCountAdvocacy() {
		return visitorCountAdvocacy;
	}

	public void setVisitorCountAdvocacy(int visitorCountAdvocacy) {
		this.visitorCountAdvocacy = visitorCountAdvocacy;
	}

	public int getContactCountAwareness() {
		return contactCountAwareness;
	}

	public void setContactCountAwareness(int contactCountAwareness) {
		this.contactCountAwareness = contactCountAwareness;
	}

	public int getContactCountAttraction() {
		return contactCountAttraction;
	}

	public void setContactCountAttraction(int contactCountAttraction) {
		this.contactCountAttraction = contactCountAttraction;
	}

	public int getContactCountAsk() {
		return contactCountAsk;
	}

	public void setContactCountAsk(int contactCountAsk) {
		this.contactCountAsk = contactCountAsk;
	}

	public int getContactCountAction() {
		return contactCountAction;
	}

	public void setContactCountAction(int contactCountAction) {
		this.contactCountAction = contactCountAction;
	}

	public int getContactCountAdvocacy() {
		return contactCountAdvocacy;
	}

	public void setContactCountAdvocacy(int contactCountAdvocacy) {
		this.contactCountAdvocacy = contactCountAdvocacy;
	}
	
	@Override
	public List<List<Integer>> getDataForFunnelGraph(){
		List<List<Integer>> data = new ArrayList<List<Integer>>(5);
		data.add(Arrays.asList(visitorCountAwareness, contactCountAwareness));
		data.add(Arrays.asList(visitorCountAttraction, contactCountAttraction));
		data.add(Arrays.asList(visitorCountAsk, contactCountAsk));
		data.add(Arrays.asList(visitorCountAction, contactCountAction));
		data.add(Arrays.asList(visitorCountAdvocacy, contactCountAdvocacy));
		return data;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
}
