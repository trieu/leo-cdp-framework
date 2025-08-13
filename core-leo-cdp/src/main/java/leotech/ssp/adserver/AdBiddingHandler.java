package leotech.ssp.adserver;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.atg.openssp.common.core.entry.CoreSupplyServlet;
import com.atg.openssp.common.core.exchange.Exchange;
import com.atg.openssp.common.exception.RequestException;
import com.rfxlab.ssp.core.system.vertx.VertxHttpServletRequest;
import com.rfxlab.ssp.core.system.vertx.VertxHttpServletResponse;

import io.vertx.ext.web.RoutingContext;

public class AdBiddingHandler extends CoreSupplyServlet<LocalSessionAgent> {

	private static final long serialVersionUID = 1L;

	@Override
	protected LocalSessionAgent getAgent(final HttpServletRequest request, final HttpServletResponse response)
			throws RequestException {
		return new LocalSessionAgent(request, response);
	}

	@Override
	protected Exchange<LocalSessionAgent> getServer() {
		return new LocalExchangeServer();
	}

	public AdBiddingHandler() throws ServletException {
		init();
	}

	public void handle(final VertxHttpServletRequest request, final VertxHttpServletResponse response)
			throws ServletException, IOException {
		String uri = request.getRequestURI();
		String site = request.getParameter("site");
		System.out.println("URI: " + uri);
		if (site != null) {
			try {
				doGet(request, response);
			} catch (Exception e) {
				e.printStackTrace();
				response.getWriter().write("error");
			}
		}
		response.flushBuffer();
		response.writeToVertx();
	}

	public static void handle(RoutingContext ctx) {
		try {
			new AdBiddingHandler().handle(new VertxHttpServletRequest(ctx), new VertxHttpServletResponse(ctx));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}