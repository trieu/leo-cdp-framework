package leotech.ssp.adserver;

import com.atg.openssp.common.core.entry.SessionAgent;
import com.atg.openssp.common.demand.Supplier;
import com.rfxlab.ssp.core.system.vertx.RequestResponseHelper;

import openrtb.bidrequest.model.BidRequest;
import openrtb.bidrequest.model.Device;
import openrtb.bidrequest.model.Gender;
import openrtb.bidrequest.model.Geo;
import openrtb.bidrequest.model.Impression;
import openrtb.bidrequest.model.User;
import openrtb.bidrequest.model.Video;
import openrtb.tables.VideoBidResponseProtocol;

public class BidRequestBuilder {
	/**
	 * Build a request object regarding to the OpenRTB Specification.
	 * 
	 * @return {@see BidRequest}
	 */
	public static BidRequest build(final SessionAgent agent) {

		final BidRequest bidRequest = new BidRequest.Builder().setId(agent.getRequestid())
				.setSite(agent.getParamValues().getSite())
				.setDevice(new Device.Builder().setGeo(new Geo.Builder().setCity("Hamburg").setCountry("DEU")
						.setLat(53.563452f).setLon(9.925742f).setZip("22761").build()).build())
				.addImp(new Impression.Builder().setId("1")
						.setVideo(new Video.Builder().addMime("application/x-shockwave-flash").setH(400).setW(600)
								.setMaxduration(100).setMinduration(30)
								.addProtocol(VideoBidResponseProtocol.VAST_2_0.getValue()).setStartdelay(1).build())
						.build())
				.setUser(new User.Builder().setBuyeruid("HHcFrt-76Gh4aPl").setGender(Gender.MALE).setId("99")
						.setYob(1981).build())
				.build();

		return bidRequest;
	}

	public static BidRequest buildInstreamBidding(final SessionAgent agent) {
		final BidRequest bidRequest = new BidRequest.Builder().setId(agent.getRequestid())
				.setSite(agent.getParamValues().getSite())
				.setDevice(new Device.Builder().setGeo(new Geo.Builder().setCity("Hamburg").setCountry("DEU")
						.setLat(53.563452f).setLon(9.925742f).setZip("22761").build()).build())
				.addImp(new Impression.Builder().setId("1")
						.setVideo(new Video.Builder().addMime("application/x-shockwave-flash").setH(400).setW(600)
								.setMaxduration(100).setMinduration(30)
								.addProtocol(VideoBidResponseProtocol.VAST_2_0.getValue()).setStartdelay(1).build())
						.build())
				.setUser(new User.Builder().setBuyeruid("HHcFrt-76Gh4aPl").setGender(Gender.MALE).setId("99")
						.setYob(1981).build())
				.build();
		return bidRequest;

	}

	public static BidRequest buildDisplayBannerBidding(final SessionAgent agent) {
		final float impFloor = 0.81f;
		final float dealFloor1 = 3.f;
		final float dealFloor2 = 2.8f;
		final String currency = "USD";

		final String deal_id_1 = "998877";
		final String deal_id_2 = "998866";

		Supplier supplier1 = new Supplier();
		supplier1.setShortName("dsp1");
		supplier1.setSupplierId(1l);
		supplier1.setContentType("application/json");
		supplier1.setOpenRtbVersion("2.4");

		// bidrequest1
		// bidresponse, price in USD
		final float bidPrice1 = 3.5f;
		final BidRequest bidRequest = RequestResponseHelper.createRequest(impFloor, dealFloor1, currency, deal_id_1, 1)
				.build();

		return bidRequest;
	}

	public static BidRequest buildOutstreamBidding(final SessionAgent agent) {
		final float impFloor = 0.81f;
		final float dealFloor1 = 3.f;
		final float dealFloor2 = 2.8f;
		final String currency = "USD";

		final String deal_id_1 = "998877";
		final String deal_id_2 = "998866";

		Supplier supplier1 = new Supplier();
		supplier1.setShortName("dsp1");
		supplier1.setSupplierId(1l);
		supplier1.setContentType("application/json");
		supplier1.setOpenRtbVersion("2.4");

		// bidrequest1
		// bidresponse, price in USD
		final float bidPrice1 = 3.5f;
		final BidRequest bidRequest = RequestResponseHelper.createRequest(impFloor, dealFloor1, currency, deal_id_1, 1)
				.build();
		return bidRequest;
	}

}
