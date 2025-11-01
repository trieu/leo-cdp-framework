package leotech.system.domain;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import io.vertx.core.json.JsonObject;
import leotech.cdp.domain.AgentManagement;
import leotech.cdp.domain.AssetCategoryManagement;
import leotech.cdp.domain.JourneyMapManagement;
import leotech.cdp.domain.schema.JourneyFlowSchema;
import leotech.system.dao.SystemUserDaoUtil;
import leotech.system.model.SystemUser;
import leotech.system.util.RedisClient;
import leotech.system.util.TaskRunner;
import leotech.system.util.database.InitDatabaseSchema;
import leotech.system.version.SystemMetaData;
import redis.clients.jedis.JedisPool;
import rfx.core.job.ScheduledJob;
import rfx.core.nosql.jedis.RedisClientFactory;
import rfx.core.util.StringPool;
import rfx.core.util.StringUtil;
import rfx.core.util.Utils;

/**
 * the system control of superadmin
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public final class SystemControl {
	
	private static final String REDIS_PUB_SUB_QUEUE = "pubSubQueue";
	private static final String MAIN_CDP_QUEUE = "main-cdp-queue";
	private static final String UPGRADE_SYSTEM = "upgrade-system";
	static JedisPool jedisPool = RedisClientFactory.buildRedisPool(REDIS_PUB_SUB_QUEUE);

	/**
	 * to set-up new database for a new  CDP system instances
	 * 
	 * @param superAdminPassword
	 */
	public final static void setupNewSystem(String superAdminPassword) {
		boolean isNewSystem = !InitDatabaseSchema.isSystemDbReady();
		if (isNewSystem) {
			setupNewSystem(superAdminPassword, false);
		}
	}

	/**
	 * get super admin password to set-up system
	 * 
	 * @param args
	 */
	public final static void setupNewSystem(String superAdminPassword, boolean autoExitSystem) {

		String editionCode = SystemMetaData.BUILD_EDITION;
		String databaseKey = SystemMetaData.MAIN_DATABASE_CONFIG;
		String superAdminEmail = SystemMetaData.SUPER_ADMIN_EMAIL;

		InitDatabaseSchema.checkAndCreateDbCollections(editionCode, databaseKey, superAdminEmail, superAdminPassword);

		if (autoExitSystem) {
			Utils.exitSystemAfterTimeout(3210);
		}
	}

	/**
	 * get super admin password to set-up system
	 * 
	 * @param args
	 */
	public final static void upgradeSystem(boolean autoExitSystem, boolean resetAllSystemConfigs,  boolean resetAllAgentsConfigs, boolean upgradeJourneyMap, String jobClasspath) {
		String editionCode = SystemMetaData.BUILD_EDITION;
		String databaseKey = SystemMetaData.MAIN_DATABASE_CONFIG;
		try {
			// check and create database collections
			InitDatabaseSchema.checkAndCreateDbCollections(editionCode, databaseKey, "", "");
			
			// system configs
			SystemConfigsManagement.initDefaultSystemData(resetAllSystemConfigs);
			
			// AI Agent service
			AgentManagement.initDefaultData(resetAllAgentsConfigs);
			
			
			
			SystemUser superAdmin = SystemUserDaoUtil.getByUserLogin(SystemUser.SUPER_ADMIN_LOGIN);
			AssetCategoryManagement.initDefaultSystemData(superAdmin);
			
			if(upgradeJourneyMap) {
				// Journey Data Flow
				JourneyFlowSchema.upgradeDefaultSystemData(true);
				JourneyMapManagement.upgradeDefaultSystemData();
			}
			else {
				// journey map
				JourneyFlowSchema.upgradeDefaultSystemData(false);
			}
			
			if(StringUtil.isNotEmpty(jobClasspath)) {
				Class<?> clazz = Class.forName(jobClasspath);
				ScheduledJob job = (ScheduledJob) clazz.getDeclaredConstructor().newInstance();
				job.doTheJob();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (autoExitSystem) {
			Utils.exitSystemAfterTimeout(5000);
		}
	}

	/**
	 * to upgrade index database
	 */
	public static void upgradeIndexDatabase() {
		JourneyFlowSchema.upgradeDefaultSystemData(true);
		Utils.exitSystemAfterTimeout(3210);
	}
	
	private static class StreamGobbler implements Runnable {
	    private InputStream inputStream;
	    private Consumer<String> consumer;
	    public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
	        this.inputStream = inputStream;
	        this.consumer = consumer;
	    }
	    @Override
	    public void run() {
	        new BufferedReader(new InputStreamReader(inputStream)).lines().forEach(consumer);
	    }
	}
	 
	/**
	 * @param user
	 * @param uri
	 * @param paramJson
	 */
	public static String sendCommandToUpgradeSystem(SystemUser user, String uri, JsonObject paramJson) {
		// TODO
		SystemEventManagement.log(user, SystemControl.class, uri, paramJson);
		return runUpdateShellScript();
		// RedisClient.enqueue(jedisPool, QUEUE_LEOSYSTEM, UPGRADE_LEOCDP);
	}
	
	/**
	 * 
	 */
	public static void monitorMainCdpQueue() {
		TaskRunner.timerSetScheduledJob(new ScheduledJob() {
			@Override
			public void doTheJob() {
				RedisClient.dequeue(MAIN_CDP_QUEUE, rs->{
					if(UPGRADE_SYSTEM.equals(rs)) {
						runUpdateShellScript();
					}
				});
			}
		});
		Utils.foreverLoop();
	}
	
	/**
	 * @return
	 */
	public static String runUpdateShellScript() {
		String shPath = SystemMetaData.UPDATE_SHELL_SCRIPT_PATH;
		if(shPath.isBlank()) {
			return "updateShellScriptPath is not found or empty";
		}
		StringBuilder o = new StringBuilder();
		try {
			ProcessBuilder builder = new ProcessBuilder();
			builder.command("sh", "-c", shPath);
			Process process = builder.start();
			StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), s -> {
				o.append(s).append(StringPool.NEW_LINE);
			});
			Executors.newSingleThreadExecutor().submit(streamGobbler);
			// termination.
			int exitCode = process.waitFor();
			if(exitCode != 0) {
				o.append("Error Code " + exitCode);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
		return o.toString();
	}
	
	

}
