package leotech.system;

import java.lang.reflect.Constructor;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.shareddata.SharedData;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import leotech.cdp.model.customer.ProfileModelUtil;
import leotech.starter.router.NotifyUserHandler;
import leotech.system.common.BaseHttpRouter;
import leotech.system.config.HttpRoutingConfigs;
import leotech.system.domain.WebSocketDataService;
import leotech.system.util.LogUtil;
import leotech.system.util.database.ArangoDbUtil;
import leotech.system.util.keycloak.KeycloakClientSsoRouter;
import leotech.system.version.SystemMetaData;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;
import rfx.core.nosql.jedis.RedisClientFactory;
import rfx.core.nosql.jedis.RedisCommand;
import rfx.core.stream.node.worker.BaseWorker;

/**
 * Http Worker for all instances of CDP worker
 * 
 * @author tantrieuf31
 * @since 2019
 *
 */
public final class HttpWorker extends BaseWorker {
	
	static final String WS_ADDRESS_NOTIFICATIONS = "notifications\\..+";

	static Logger logger = LoggerFactory.getLogger(HttpWorker.class);

	static final String URI_WS_EVENT_BUS = "/eventbus*";
	static HttpWorker instance = null;
	final HttpRoutingConfigs httpRoutingConfigs;
	final String defaultDbConfig;
	private static JedisPool jedisPool =  RedisClientFactory.buildRedisPool("realtimeDataStats");
	
	public final HttpRoutingConfigs getHttpRoutingConfigs() {
		return httpRoutingConfigs;
	}

	public final String getDefaultDbConfig() {
		return defaultDbConfig;
	}

	protected HttpWorker(String workerName) {
		super(workerName);
		httpRoutingConfigs = HttpRoutingConfigs.load(workerName);
		defaultDbConfig = httpRoutingConfigs.getDefaultDbConfig();
		logger.info("HttpWorker.defaultDbConfig " + defaultDbConfig);
		// check to make sure httpRoutingConfigs is not null
		if (httpRoutingConfigs == null) {
			String errorMsg = workerName + " is not existed in in " + HttpRoutingConfigs.FILE_HTTP_ROUTING_CONFIGS_JSON;
			throw new IllegalArgumentException(errorMsg);
		} else {
			LogUtil.logInfo(HttpWorker.class, "loaded config " + new Gson().toJson(httpRoutingConfigs));
		}
	}

	/**
	 * Create new AdDeliveryWorker instance with implemented httpHandler
	 * 
	 * @param host
	 * @param port
	 * @param httpHandler
	 */
	public final static void start(String workerName) {
		SystemMetaData.initTimeZoneGMT();
		
		System.setProperty("vertx.disableFileCPResolving", "true");

		instance = new HttpWorker(workerName);
		
		// check and init database
		ArangoDbUtil.initActiveArangoDatabase(instance.httpRoutingConfigs.getDefaultDbConfig());
		
		// init profile model metadata
		ProfileModelUtil.init();
		
		// start HTTP service
		String host = instance.httpRoutingConfigs.getHost();
		int port = instance.httpRoutingConfigs.getPort();
		instance.start(host, port);
		
	}

	@Override
	protected void onStartDone() {
		LogUtil.logInfo(this.getClass(),name + " is loaded ...");
	}

	public static HttpWorker getInstance() {
		if (instance == null) {
			throw new IllegalAccessError("startNewInstance must called before getInstance");
		}
		return instance;
	}

	@Override
	public final void start(final String host, final int port) {
		Router router = Router.router(vertxInstance);

		// for HTTP POST upload or Ajax POST submit
		if (httpRoutingConfigs.isBodyHandlerEnabled()) {
			// the system must using POST as JsonDataPayload
			router.route().handler(BodyHandler.create());
		}

		// WebSocket
		if (httpRoutingConfigs.isSockJsHandlerEnabled()) {
			initEventBusHandler(router);
		}
		
		// SSO router
		KeycloakClientSsoRouter.startKeyCloakRouter(vertxInstance, router);
		
		// 
		String className = httpRoutingConfigs.getClassNameHttpRouter();
		Constructor<?> constructor = initRouterClass(className, host, port);
		if(constructor != null) {
			// find the class and create new instance
			router.route().handler(context -> {
				try {
					Object instance = constructor.newInstance(context, host, port);
					BaseHttpRouter obj = (BaseHttpRouter) instance;
					obj.process();
				} catch (Throwable e) {
					e.printStackTrace();
					String err = e.getMessage();
					context.response().setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR).end(err);
				}
			});

			
			HttpServer server = checkAndCreateHttpServer(host, port);
			if (server == null) {
				System.err.println("registerWorkerHttpRouter return NULL value");
				return;
			}
			server.requestHandler(router).listen(port, host);
			registerWorkerNodeIntoCluster();
		}
		else {
			logger.error(className + " is NULL when getConstructor from initRouterClass !");
		}

		
	}

	private Constructor<?> initRouterClass(String className, final String host, final int port) {
		String nodeId = String.format("[%s:%d]", host, port);
		Constructor<?> constructor = null;
		try {
			constructor = Class.forName(className).getConstructor(RoutingContext.class, String.class, Integer.class);
		} catch (Throwable e1) {
			e1.printStackTrace();
			logger.error(className + ":" + e1.getMessage());
		}
		new RedisCommand<Void>(jedisPool) {
			@Override
			protected Void build(Jedis jedis) throws JedisException {
				jedis.hset(className, nodeId, "0");
				return null;
			}
		}.execute();
		return constructor;
	}

	/**
	 * @return
	 */
	final void initEventBusHandler(Router router) {
		 // EventBus bridge options
	    BridgeOptions opts = new BridgeOptions()
		  .addInboundPermitted(new PermittedOptions().setAddressRegex(WS_ADDRESS_NOTIFICATIONS))  // Allow inbound messages to any notification address
		  .addOutboundPermitted(new PermittedOptions().setAddressRegex(WS_ADDRESS_NOTIFICATIONS));  // Allow outbound messages from any notification address

		SharedData data =  this.vertxInstance.sharedData();
		WebSocketDataService repository = new WebSocketDataService(data);
		EventBus eventBus = this.vertxInstance.eventBus();
		NotifyUserHandler notifySystemUserHandler = new NotifyUserHandler(eventBus, repository);

		SockJSHandler sockJSHandler = SockJSHandler.create(vertxInstance);
		sockJSHandler.bridge(opts, notifySystemUserHandler);
		router.route(URI_WS_EVENT_BUS).handler(sockJSHandler);
	}

}
