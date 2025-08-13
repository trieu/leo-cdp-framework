package leotech.ssp.adserver.chanel;

import java.util.Optional;
import java.util.concurrent.Callable;

import com.atg.openssp.common.core.entry.SessionAgent;
import com.atg.openssp.common.demand.ParamValue;
import com.atg.openssp.common.provider.AdProviderReader;

import util.math.FloatComparator;

/**
 * @author TrieuNT
 *
 */
public class AdServingService implements Callable<AdProviderReader> {

	private final LocalAdBroker broker;

	private final SessionAgent agent;

	/**
	 * 
	 * @param agent {@link SessionAgent}
	 */
	public AdServingService(final SessionAgent agent) {
		this.agent = agent;
		broker = new LocalAdBroker();
		broker.setSessionAgent(agent);
	}

	/**
	 * Calls the Broker for Adserver.
	 * 
	 * @return {@link AdProviderReader}
	 */
	@Override
	public AdProviderReader call() throws Exception {
		try {
			ParamValue paramValues = agent.getParamValues();

			final Optional<AdProviderReader> adProvider = broker.call(paramValues);
			System.out.println("AdServingService " + adProvider.get());
			if (adProvider.isPresent()) {
				final AdProviderReader provider = adProvider.get();

				// check if the ad response price is greator or equal the floorprice
				float bidfloorPrice = paramValues.getVideoad().getBidfloorPrice();
				if (FloatComparator.greaterOrEqual(provider.getPriceEur(), bidfloorPrice)) {
					return provider;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

}
