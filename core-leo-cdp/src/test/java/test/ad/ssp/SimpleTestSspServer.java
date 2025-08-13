package test.ad.ssp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atg.openssp.common.provider.AdProviderReader;
import com.rfxlab.ssp.core.system.vertx.SspDataLoader;
import com.rfxlab.ssp.core.system.vertx.VertxHttpServletRequest;
import com.rfxlab.ssp.core.system.vertx.VertxHttpServletResponse;

import channel.adserving.AdserverLocalBroker;
import channel.adserving.AdservingCampaignProvider;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import leotech.ssp.adserver.AdBiddingHandler;

public class SimpleTestSspServer {

	private static final Logger log = LoggerFactory.getLogger(SimpleTestSspServer.class);

	public static void main(String[] args) {
		new SimpleTestSspServer().start();
	}

	//http://devssp?site=1&domain=size_1.com&h=600&w=800&publisher=pubisher_1
	public void start() {
		Vertx vertx = Vertx.vertx();
		Router router = Router.router(vertx);
		router.route().handler(BodyHandler.create());

		// initing cache data
		SspDataLoader.init();
		AdProviderReader adProvider = new AdservingCampaignProvider(true, 1.5F, 2082, "https://example.com/vast3?li=2082");
		AdserverLocalBroker.setTestableAdProvider(adProvider);
		
		router.route().handler(context -> {
			VertxHttpServletRequest request = new VertxHttpServletRequest(context);
			VertxHttpServletResponse response = new VertxHttpServletResponse(context);
			handlerSspRequest(request, response);
		});

		HttpServer server = vertx.createHttpServer();
		server.requestHandler(router);
		server.listen(9790);
	}

	void handlerSspRequest(final VertxHttpServletRequest request, final VertxHttpServletResponse response) {
		try {
			new AdBiddingHandler().handle(request, response);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
