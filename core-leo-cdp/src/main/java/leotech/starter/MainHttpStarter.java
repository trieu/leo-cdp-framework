package leotech.starter;

import com.itfsw.query.builder.support.parser.AbstractArangoQueryRuleParser;

import leotech.system.HttpWorker;
import leotech.system.domain.SystemControl;
import leotech.system.util.LogUtil;
import leotech.system.version.SystemMetaData;

/**
 * the main starter for an instance of service, defined in the
 * configs/http-routing-configs.json
 * 
 * @author tantrieuf31
 * @since 2020
 *
 */
public final class MainHttpStarter {

	static final String HELP = "help";

	public static final String SETUP_NEW_SYSTEM = "setup-system-with-password";

	public static final String UPGRADE_SYSTEM = "upgrade-system";

	public static final String UPGRADE_SYSTEM_AND_RESET_CONFIGS = "upgrade-system-and-reset-configs";

	public static final String UPGRADE_AI_AGENT_AND_RESET_CONFIGS = "upgrade-ai-agent-and-reset-configs";

	public static final String UPGRADE_SYSTEM_AND_DATA = "upgrade-system-and-data";

	public static final String UPGRADE_INDEX_DATABASE = "upgrade-index-database";

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		LogUtil.loadLoggerConfigs();
		SystemMetaData.initTimeZoneGMT();
		AbstractArangoQueryRuleParser.setTimeZone(SystemMetaData.DEFAULT_TIME_ZONE);

		int length = args.length;

		if (length == 0) {
			// default key: httpRoutingConfigAdmin in configs/http-routing-configs.json
			HttpWorker.start(SystemMetaData.HTTP_ROUTING_CONFIG_ADMIN);
		} else if (length >= 1) {
			String command = args[0];

			if (length == 1) {
				// start main worker with key in configs/http-routing-configs.json
				if (UPGRADE_SYSTEM.equalsIgnoreCase(command)) {
					SystemControl.upgradeSystem(true, false, false, false, null);
					return;
				}
				if (UPGRADE_SYSTEM_AND_RESET_CONFIGS.equalsIgnoreCase(command)) {
					SystemControl.upgradeSystem(true, true, false, false, null);
					return;
				} else if (UPGRADE_AI_AGENT_AND_RESET_CONFIGS.equalsIgnoreCase(command)) {
					SystemControl.upgradeSystem(true, false, true, false, null);
					return;
				} else if (UPGRADE_INDEX_DATABASE.equalsIgnoreCase(command)) {
					SystemControl.upgradeIndexDatabase();
					return;
				} else if (command.contains(HELP)) {
					printHelpInfo();
				} else {
					HttpWorker.start(args[0]);
				}
			} else if (length == 2) {
				// start the process to setup database for first-time
				if (SETUP_NEW_SYSTEM.equalsIgnoreCase(command)) {
					String superAdminPassword = args[1];
					SystemControl.setupNewSystem(superAdminPassword, true);
					return;
				} else if (UPGRADE_SYSTEM_AND_DATA.equalsIgnoreCase(command)) {
					String jobClasspath = args[1];
					SystemControl.upgradeSystem(true, false, false, false, jobClasspath);
					return;
				} else {
					LogUtil.println("the first argument should be " + SETUP_NEW_SYSTEM);
				}
			} else {
				printHelpInfo();
			}
		} else {
			printHelpInfo();
		}
	}

	protected static void printHelpInfo() {
		LogUtil.println("Invalid arguments");
		LogUtil.println("Please pass a valid argument from: ");
		LogUtil.println("SETUP_NEW_SYSTEM: " + SETUP_NEW_SYSTEM);

		LogUtil.println("UPGRADE_SYSTEM: " + UPGRADE_SYSTEM);
		LogUtil.println("UPGRADE_SYSTEM_AND_RESET_CONFIGS: " + UPGRADE_SYSTEM_AND_RESET_CONFIGS);
		LogUtil.println("UPGRADE_INDEX_DATABASE: " + UPGRADE_INDEX_DATABASE);

		LogUtil.println("UPGRADE_SYSTEM_AND_DATA: " + UPGRADE_SYSTEM_AND_DATA + " [jobClasspath]");
	}

}