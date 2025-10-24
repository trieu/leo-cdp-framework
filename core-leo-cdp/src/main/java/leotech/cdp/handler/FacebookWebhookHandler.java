package leotech.cdp.handler;


import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import leotech.cdp.domain.TouchpointHubManagement;
import leotech.cdp.domain.WebhookDataManagement;
import leotech.cdp.model.analytics.WebhookDataEvent;
import leotech.cdp.model.journey.EventObserver;
import leotech.cdp.model.journey.TouchpointHub;
import leotech.system.common.BaseHttpHandler;
import leotech.system.common.BaseHttpRouter;
import leotech.system.util.HttpWebParamUtil;
import rfx.core.util.StringUtil;

/**
 * @author Trieu Nguyen
 * @since 2025
 *
 */
public final class FacebookWebhookHandler {
	
	private static final String MODE_SUBSCRIBE = "subscribe";

	private static final String ERROR_VERIFICATION_FAILED = "{\"error\": \"Verification failed\"}";

	public static final String FACEBOOK = "/facebook";

    private static final Logger logger = LoggerFactory.getLogger(FacebookWebhookHandler.class);


    public static boolean handle(EventObserver observer, RoutingContext context, HttpServerRequest req, String urlPath,
                                 MultiMap reqHeaders, MultiMap params, HttpServerResponse resp, MultiMap outHeaders,
                                 String origin) {
        outHeaders.set(CONTENT_TYPE, BaseHttpHandler.CONTENT_TYPE_TEXT);
        BaseHttpRouter.setCorsHeaders(outHeaders, origin);

        String mode = StringUtil.safeString(params.get("hub.mode"));
        String tokenValue = StringUtil.safeString(params.get("hub.verify_token"));
        String challenge = StringUtil.safeString(params.get("hub.challenge"));
        
        String observerId = observer.getId();
		boolean isValidToken = observer.getAccessTokens().getOrDefault(observerId , "").equals(tokenValue);  
        
        String payload = StringUtil.safeString(context.getBodyAsString());
   
        String source = HttpWebParamUtil.getString(params, HttpParamKey.SOURCE);
		TouchpointHub tphub = TouchpointHubManagement.getByObserverId(observerId);

		// FB verify
        if (! challenge.isBlank() && MODE_SUBSCRIBE.equals(mode) && StringUtil.isNotEmpty(tokenValue) && isValidToken) {
            logger.info("FB Webhook verified successfully.");
            resp.setStatusCode(200).end(challenge);
            return true;
        } 
		else if (tphub != null && !source.isEmpty() ) {
			// FB webhook HTTP post
			WebhookDataEvent e = new WebhookDataEvent(source, observerId, tphub.getId(), payload);
			WebhookDataManagement.process(source, e);
			return true;
		}
        else {
            logger.warn("FB Webhook verification failed.");
            resp.setStatusCode(403).end(ERROR_VERIFICATION_FAILED);
            return false;
        }
    }
}
