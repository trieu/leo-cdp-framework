package leotech.cdp.domain;

import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import leotech.cdp.dao.WebhookDataEventDao;
import leotech.cdp.domain.processor.ZaloWebhookProcessor;
import leotech.cdp.model.analytics.WebhookDataEvent;
import leotech.system.util.TaskRunner;

/**
 * Webhook Data Event Management
 * 
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public class WebhookDataManagement {
	
	 private static final Logger logger = LoggerFactory.getLogger(WebhookDataManagement.class);
	
	// Map sources to their corresponding event processor
	private static final Map<String, Consumer<WebhookDataEvent>> PROCESSORS = Map.of(
	    ZaloWebhookProcessor.ZALO, ZaloWebhookProcessor::processWebhookEvent
	);


	
	/**
	 * save and the process WebhookDataEvent
	 * 
	 * @param e
	 * @return id of WebhookDataEvent
	 */
	public static void save(WebhookDataEvent e) {
		TaskRunner.runInThreadPools(()->{
			String id = WebhookDataEventDao.save(e);
			System.out.println("WebhookDataEvent is saved OK with ID: " + id);
		});
	}

	/**
	 * @param source
	 * @param e
	 */
	public static void process(String source, WebhookDataEvent e) {
		// save DB first
		save(e);
		
		// the transform event and process for Data Unification
		// Transform event and process for Data Unification
		TaskRunner.runInThreadPools(() -> {
		    Consumer<WebhookDataEvent> processor = PROCESSORS.get(source.toLowerCase());
		    if (processor != null) {
		        processor.accept(e);
		    } else {
		    	logger.error("[ERROR] No processor for WebhookDataEvent.source: {}", source);
		    }
		});
	}
	
}
