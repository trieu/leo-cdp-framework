package leotech.starter.router;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.ext.web.handler.sockjs.BridgeEvent;
import leotech.system.domain.WebSocketDataService;

/**
 * 
 * https://vertx.io/blog/real-time-bidding-with-websockets-and-vert-x/
 * https://medium.com/@pvub/real-time-reactive-micro-service-performance-monitoring-vert-x-sockjs-f0e904d9ca8d
 * 
 * @author tantrieuf31
 *
 */
public class WebSocketDataHandler implements Handler<BridgeEvent>{
    private static final Logger logger = LoggerFactory.getLogger(WebSocketDataHandler.class);
    private final EventBus eventBus;
    private final WebSocketDataService repository;

    public WebSocketDataHandler(EventBus eventBus, WebSocketDataService repository) {
        this.eventBus = eventBus;
        this.repository = repository;
    }

    @Override
    public void handle(BridgeEvent event) {
	
        if (event.type() == BridgeEventType.SOCKET_CREATED)
            logger.info("A socket was created");

        if (event.type() == BridgeEventType.SEND)
            clientToServer();

        event.complete(true);
    }

    private void clientToServer() {
        Optional<Integer> counter = repository.get();
        if (counter.isPresent()) {
            Integer value = counter.get() + 1;
            repository.update(value);
            eventBus.publish("out", value);
        } else {
            Integer value = 1;
            repository.update(value);
            eventBus.publish("out", value);
        }
    }
}
