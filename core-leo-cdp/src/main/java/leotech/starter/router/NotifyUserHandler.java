package leotech.starter.router;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.ext.web.handler.sockjs.BridgeEvent;
import leotech.system.common.SecuredHttpDataHandler;
import leotech.system.dao.SystemUserDaoUtil;
import leotech.system.domain.WebSocketDataService;
import leotech.system.model.Notification;
import leotech.system.model.SystemUser;
import leotech.system.version.SystemMetaData;
import rfx.core.util.StringUtil;
import rfx.core.util.Utils;

/**
 * 
 * https://vertx.io/blog/real-time-bidding-with-websockets-and-vert-x/
 * https://medium.com/@pvub/real-time-reactive-micro-service-performance-monitoring-vert-x-sockjs-f0e904d9ca8d
 * 
 * @author tantrieuf31
 *
 */
public class NotifyUserHandler implements Handler<BridgeEvent> {
	
	static final int TIME_TO_CHECK = 12000;
	static final AtomicBoolean TIMER_STARTED = new AtomicBoolean(false);
	static final Timer TIMER = new Timer(true);
	
	private static final String BODY = "body";
	private static final String USERSESSION = "usersession";
	private static final String ADDRESS = "address";
	
	public static final String NOTIFICATIONS = "notifications";
	public static final String REGISTER_SESSION = NOTIFICATIONS + ".register.session";

	private static final Logger logger = LoggerFactory.getLogger(NotifyUserHandler.class);
	
	final EventBus eventBus;
	final WebSocketDataService repository;
	
	static Set<String> notificationQueueUserIds = new HashSet<>();

	public NotifyUserHandler(EventBus eventBus, WebSocketDataService repository) {
		this.eventBus = eventBus;
		this.repository = repository;
		startTimer();
	}
	
	private void startTimer() {
		if(!TIMER_STARTED.get()) {
			TIMER_STARTED.set(true);
			TIMER.schedule(new TimerTask() {
				@Override
				public void run() {
					checkNotification();
				}
			}, TIME_TO_CHECK, TIME_TO_CHECK);
		}
	}
	
	private void checkNotification() {
		if(SystemMetaData.isDevMode()) {
			// logger.info("NotifyUserHandler.checkNotificationQueue at " + new Date());
		}
		notificationQueueUserIds.forEach(userId->{
			flushAllNotifications(userId);
		});
	}
	
	/**
	 * @param user.key or System User ID
	 */
	public static void checkNotification(String userKey) {
		if(TIMER_STARTED.get()) {
			notificationQueueUserIds.add(userKey);
		}
		else {
			logger.error("notificationQueueUserIds is FULL");
		}
	}
	
	/**
	 * @param userId
	 */
	private void flushAllNotifications(String userId) {
		//logger.info("flushAllNotifications " + userId);
		if(userId != null) {
			SystemUser u = SystemUserDaoUtil.getSystemUserByKey(userId);
			if(u != null) {
				List<Notification> notifications= u.getNotifications();
				//logger.info("notifications.size " + notifications.size());
				for (Notification n : notifications) {
					if(n.isNew()) {
						sendNotification(userId, n);
					}
				}
				SystemUserDaoUtil.updateSystemUser(u);
			}
		}
	}

	/**
	 * @param systemUserId
	 * @param msg
	 */
	private void sendNotification(String userKey, Notification n) {
		if(eventBus != null && StringUtil.isNotEmpty(userKey)) {
			String registerAddress = "notifications." + userKey;	
			eventBus.publish(registerAddress, n.toJsonObject());
			n.setStatus(Notification.STATUS_SENT);
			logger.info(userKey + " is sent a notification " + n);
			Utils.sleep(200);
		}
		else {
			logger.error("Error when sendNotification eventBus:" + eventBus + " userKey " + userKey);
		}
	}

	/**
	 *
	 */
	@Override
	public void handle(BridgeEvent event) {
		BridgeEventType type = event.type();
		if (type == BridgeEventType.SOCKET_CREATED) {
			//logger.info("A socket was created");
		}
		else if (type == BridgeEventType.SEND) {
			JsonObject message = event.getRawMessage();
			String address = message.getString(ADDRESS);
			
			if(REGISTER_SESSION.equals(address)) {
				JsonObject body = message.getJsonObject(BODY);
				String usersession = body.getString(USERSESSION);
				String registerAddress = body.getString(ADDRESS);

				SystemUser user = SecuredHttpDataHandler.getSystemUserFromSession(usersession);
				if(user != null) {
					//logger.info(" User registered with usersession: " + usersession + " at address: " + registerAddress);
					
					JsonObject ok = Notification.registeredOk(user.getUserLogin()).toJsonObject();
					eventBus.publish(registerAddress, ok);
					checkNotification(user.getKey());
				}
			}
			
		}
		event.complete(true);
	}
	
}
