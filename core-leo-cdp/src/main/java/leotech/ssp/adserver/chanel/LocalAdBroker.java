package leotech.ssp.adserver.chanel;

import java.util.Optional;

import com.atg.openssp.common.core.broker.AbstractBroker;
import com.atg.openssp.common.demand.ParamValue;
import com.atg.openssp.common.exception.BidProcessingException;
import com.atg.openssp.common.provider.AdProviderReader;

/**
 * This class acts as Broker to the local in-memory connector. It uses an
 * in-memory database for bidding process
 * 
 * @author TrieuNT
 *
 */
public class LocalAdBroker extends AbstractBroker {

	// define database connector

	public LocalAdBroker() {
		// TODO
	}

	/**
	 * Connects to the database of local ad server.
	 * 
	 * @return Optional of {@link AdProviderReader}
	 * @throws BidProcessingException
	 */
	public Optional<AdProviderReader> call(ParamValue paramValues) throws BidProcessingException {

		// TODO
		int zoneId = paramValues.getZone().getZoneId();

		String contentType = AdProviderReader.TEXT_XML;
		AdProviderReader adProvider = new AdProvider(true, 0.5F, 2082, "https://example.com/vast3?li=2082",
				contentType);

		return Optional.ofNullable(adProvider);
	}

}
