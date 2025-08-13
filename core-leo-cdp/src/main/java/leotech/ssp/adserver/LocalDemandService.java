package leotech.ssp.adserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atg.openssp.common.cache.CurrencyCache;
import com.atg.openssp.common.core.entry.SessionAgent;
import com.atg.openssp.common.demand.BidExchange;
import com.atg.openssp.common.demand.ResponseContainer;
import com.atg.openssp.common.demand.Supplier;
import com.atg.openssp.common.exception.InvalidBidException;
import com.atg.openssp.common.provider.AdProviderReader;
import com.atg.openssp.core.cache.type.ConnectorCache;
import com.atg.openssp.core.exchange.Auction;
import com.atg.openssp.core.exchange.channel.rtb.DemandBroker;
import com.atg.openssp.core.exchange.channel.rtb.DemandExecutorServiceFacade;
import com.atg.openssp.core.exchange.channel.rtb.DemandService;
import com.atg.openssp.core.exchange.channel.rtb.OpenRtbConnector;

import openrtb.bidrequest.model.BidRequest;
import openrtb.bidrequest.model.Impression;
import openrtb.bidresponse.model.BidResponse;

/**
 * @author Trieu
 *
 */
public class LocalDemandService implements Callable<AdProviderReader> {

	private static final Logger log = LoggerFactory.getLogger(DemandService.class);

	private final SessionAgent agent;

	/**
	 * 
	 * @param {@link SessionAgent}
	 */
	public LocalDemandService(final SessionAgent agent) {
		this.agent = agent;
	}

	/**
	 * Loads the connectors for supplier from the cache.
	 * <p>
	 * Therefore it prepares the {@link BidRequest} for every connector, which is a
	 * representant to a demand connection.
	 * 
	 * @return a {@code List} with {@link DemandBroker}
	 * 
	 * @link SessionAgent
	 */
	private List<DemandBroker> loadSupplierConnectors() {
		final List<OpenRtbConnector> connectorList = ConnectorCache.instance.getAll();
		final List<DemandBroker> connectors = new ArrayList<>();

		// TODO
		final BidRequest realBidRequest = BidRequestBuilder.build(agent);

		connectorList.stream().filter(b -> b.getSupplier().getActive() == 1).forEach(connector -> {
			BidRequest bidRequest = realBidRequest;
			Supplier supplier = connector.getSupplier();

			int underTest = supplier.getUnderTest();
			if (underTest == 1) {
				bidRequest = BidRequestBuilder.buildOutstreamBidding(agent);
			}

			final DemandBroker demandBroker = new DemandBroker(supplier, connector, agent);
			if (bidRequest.getImp().get(0).getBidfloor() > 0) {
				final Impression imp = bidRequest.getImp().get(0);
				// floorprice in EUR -> multiply with rate to get target
				// currency therfore floorprice currency is always the same
				// as supplier currency
				Float currency = CurrencyCache.instance.get(supplier.getCurrency());
				imp.setBidfloor(bidRequest.getImp().get(0).getBidfloor() * currency);
				imp.setBidfloorcur(supplier.getCurrency());
			}

			bidRequest.setTest(underTest);
			demandBroker.setBidRequest(bidRequest);
			agent.getBidExchange().setBidRequest(supplier, bidRequest);
			connectors.add(demandBroker);
		});

		return connectors;
	}

	/**
	 * Calls the DSP. Collects the results of bidrequest, storing the results after
	 * validating into a {@link BidExchange} object.
	 * 
	 * <p>
	 * Principle of work is the following:
	 * <ul>
	 * <li>Loads the connectors as callables from the cache
	 * {@link DemandBroker}</li>
	 * <li>Invoke the callables due to the {@link DemandExecutorServiceFacade}</li>
	 * <li>For every result in the list of futures, the response will be validated
	 * {@link OpenRtbVideoValidator} and stored in a {@link BidExchange} object</li>
	 * <li>From the set of reponses in the {@link BidExchange} a bidding winner will
	 * be calculated in the Auction service {@link Auction}</li>
	 * </ul>
	 * <p>
	 * 
	 * @return {@link AdProviderReader}
	 * @throws Exception
	 */
	public AdProviderReader call() throws Exception {
		AdProviderReader adProvider = null;
		try {
			final List<DemandBroker> connectors = loadSupplierConnectors();
			System.out.println(connectors);
			final List<Future<ResponseContainer>> futures = DemandExecutorServiceFacade.instance.invokeAll(connectors);
			System.out.println(futures);
			futures.parallelStream().filter(Objects::nonNull).forEach(future -> {
				try {
					final ResponseContainer respContainer = future.get();

					// TODO validate bid response

					BidResponse bidResponse = respContainer.getBidResponse();
					Supplier supplier = respContainer.getSupplier();
					agent.getBidExchange().setBidResponse(supplier, bidResponse);
				} catch (final ExecutionException e) {
					log.error("ExecutionException {} {}", agent.getRequestid(), e.getMessage());
				} catch (final InterruptedException e) {
					log.error("InterruptedException {} {}", agent.getRequestid(), e.getMessage());
				} catch (final CancellationException e) {
					log.error("CancellationException {} {}", agent.getRequestid(), e.getMessage());
				}
			});

			try {
				adProvider = Auction.auctioneer(agent.getBidExchange());
			} catch (final InvalidBidException e) {
				log.error("{} {}", agent.getRequestid(), e.getMessage());
			}
		} catch (final InterruptedException e) {
			log.error(" InterruptedException (outer) {} {}", agent.getRequestid(), e.getMessage());
		}

		return adProvider;
	}
}
