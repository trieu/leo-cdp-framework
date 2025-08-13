package leotech.ssp.adserver;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atg.openssp.common.core.entry.SessionAgent;
import com.atg.openssp.common.core.exchange.Exchange;
import com.atg.openssp.common.core.exchange.ExchangeExecutorServiceFacade;
import com.atg.openssp.common.provider.AdProviderReader;
import com.google.gson.Gson;

import leotech.ssp.adserver.chanel.AdServingService;
import util.math.FloatComparator;

/**
 * This is the server which is mainly responsible to start the bidprocess,
 * collect the result and build a response for the client.
 * 
 * @author TrieuNT
 *
 */
public class LocalExchangeServer implements Exchange<LocalSessionAgent> {

	private static final Logger log = LoggerFactory.getLogger(LocalExchangeServer.class);

	@Override
	public boolean processExchange(final LocalSessionAgent agent) {
		final AdProviderReader winner = execute(agent);
		return evaluateResponse(agent, winner);
	}

	public static List<Callable<AdProviderReader>> createListOfChannels(final SessionAgent agent) {
		final List<Callable<AdProviderReader>> callables = new ArrayList<>();
		callables.add(new LocalDemandService(agent));
		callables.add(new AdServingService(agent));
		return callables;
	}

	protected AdProviderReader execute(final SessionAgent agent) {
		try {
			final List<Callable<AdProviderReader>> callables = createListOfChannels(agent);
			final List<Future<AdProviderReader>> futures = ExchangeExecutorServiceFacade.instance.invokeAll(callables);
			final Future<AdProviderReader> winner = futures.stream().reduce(LocalExchangeServer::validate).orElse(null);
			if (winner != null) {
				try {
					return winner.get();
				} catch (final ExecutionException e) {
					log.error(e.getMessage());
				}
			} else {
				log.error("no winner detected");
			}
		} catch (final InterruptedException e) {
			log.error(e.getMessage());
		}
		return null;
	}

	public static Future<AdProviderReader> validate(final Future<AdProviderReader> a,
			final Future<AdProviderReader> b) {
		try {
			System.out.println("---------------");
			System.out.println(a.getClass().getName() + " " + a.get());
			System.out.println(b.getClass().getName() + " " + b.get());
			if (b.get() == null) {
				return a;
			}
			if (a.get() == null) {
				return b;
			}

			if (FloatComparator.greaterThanWithPrecision(a.get().getPriceEur(), b.get().getPriceEur())) {
				return a;
			}
		} catch (final Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
		return b;
	}

	private boolean evaluateResponse(final SessionAgent agent, final AdProviderReader winner) {
		try (Writer out = agent.getHttpResponse().getWriter()) {
			if (winner != null && winner.isValid()) {
				System.out.println("winner " + new Gson().toJson(winner));
				final String responseData = winner.buildResponse();
				out.append(responseData);
				agent.getHttpResponse().setContentType(winner.getContentType());
				winner.perform(agent);
				out.flush();
				return true;
			} else {
				// remove this in production environmant
				if (agent.getParamValues().getIsTest().equals("1")) {
					agent.getHttpResponse().setContentType("application/json");
					final String responseData = "{\"result\":\"Success\", \"message\":\"OpenSSP is working.\"}";
					out.append(responseData);
					out.flush();
					return true;
				}
			}
		} catch (final IOException e) {
			log.error(e.getMessage());
		}
		return false;
	}

}
