package leotech.ssp.adserver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atg.openssp.common.core.entry.RequestMonitor;
import com.atg.openssp.common.core.entry.SessionAgent;
import com.atg.openssp.common.demand.BidExchange;
import com.atg.openssp.common.exception.RequestException;
import com.atg.openssp.common.logadapter.RequestLogProcessor;
import com.atg.openssp.core.exchange.RequestSessionAgent;

public class LocalSessionAgent extends SessionAgent {

	private static final Logger log = LoggerFactory.getLogger(RequestSessionAgent.class);

	/**
	 * 
	 * @param request
	 * @param response
	 * @throws RequestException
	 */
	public LocalSessionAgent(final HttpServletRequest request, final HttpServletResponse response)
			throws RequestException {
		super(request, response);
		this.paramValue = new LocalRequestValidator().validateEntryParams(request);

		log.debug(paramValue.toString());

		this.bidExchange = new BidExchange();

		RequestLogProcessor.instance.setLogData(this);
		RequestMonitor.monitorRequests();
	}

}