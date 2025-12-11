package leotech.starter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itfsw.query.builder.support.parser.AbstractArangoQueryRuleParser;
import leotech.system.HttpWorker;
import leotech.system.domain.SystemControl;
import leotech.system.util.LogUtil;
import leotech.system.version.SystemMetaData;

/**
 * The main starter for the LEO CDP service instance.
 * Configuration is defined in configs/http-routing-configs.json.
 * @author tantrieuf31
 * @since 2020
 */
public final class MainHttpStarter {

	static Logger logger = LoggerFactory.getLogger(MainHttpStarter.class);

    private static final String CMD_HELP = "help";
    private static final String CMD_SETUP_NEW_SYSTEM = "setup-system-with-password";
    
    // Upgrade Commands
    private static final String CMD_UPGRADE_SYSTEM = "upgrade-system";
    private static final String CMD_UPGRADE_RESET_CONFIGS = "upgrade-system-and-reset-configs";
    private static final String CMD_UPGRADE_AI_AGENT = "upgrade-ai-agent-and-reset-configs";
    private static final String CMD_UPGRADE_SYSTEM_DATA = "upgrade-system-and-data";
    private static final String CMD_UPGRADE_INDEX_DB = "upgrade-index-database";

    // Private constructor to prevent instantiation
    private MainHttpStarter() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Main entry point.
     * * @param args Command line arguments
     */
    public static void main(String[] args) {
        try {
            initializeSystemEnvironment();
            dispatchCommand(args);
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
            System.exit(1);
        }
    }

    private static void initializeSystemEnvironment() {
        LogUtil.loadLoggerConfigs();
        SystemMetaData.initTimeZoneGMT();
        AbstractArangoQueryRuleParser.setTimeZone(SystemMetaData.DEFAULT_TIME_ZONE);
    }

    private static void dispatchCommand(String[] args) throws Exception {
        int argCount = args.length;
        // Case 0: Default Startup
        if (argCount == 0) {
            LogUtil.println("Starting HTTP Worker with default admin config...");
            HttpWorker.start(SystemMetaData.HTTP_ROUTING_CONFIG_ADMIN);
            return;
        }

        var command = args[0].trim();

        // Case 1: Single Argument Commands
        if (argCount == 1) {
            handleSingleArgCommand(command);
            return;
        }
        // Case 2: Double Argument Commands
        if (argCount == 2) {
            var secondaryArg = args[1].trim();
            handleDoubleArgCommand(command, secondaryArg);
            return;
        }
        // Case Default: Invalid arguments
        printHelpInfo();
    }

    private static void handleSingleArgCommand(String command) throws Exception {
        // Use if-else for Java 11 compatibility (Switch expressions are Java 14+)
        if (CMD_UPGRADE_SYSTEM.equalsIgnoreCase(command)) {
            performSystemUpgrade();
        } 
        else if (CMD_UPGRADE_RESET_CONFIGS.equalsIgnoreCase(command)) {
            performSystemUpgradeWithConfigReset();
        } 
        else if (CMD_UPGRADE_AI_AGENT.equalsIgnoreCase(command)) {
            performAiAgentUpgrade();
        } 
        else if (CMD_UPGRADE_INDEX_DB.equalsIgnoreCase(command)) {
            SystemControl.upgradeIndexDatabase();
        } 
        else if (command.toLowerCase().contains(CMD_HELP)) {
            printHelpInfo();
        } 
        else {
            // Assume the argument is a custom routing config key
            LogUtil.println("Starting HTTP Worker with config key: " + command);
            HttpWorker.start(command);
        }
    }

    private static void handleDoubleArgCommand(String command, String secondaryArg) throws Exception {
        if (CMD_SETUP_NEW_SYSTEM.equalsIgnoreCase(command)) {
            var superAdminPassword = secondaryArg;
            SystemControl.setupNewSystem(superAdminPassword, true);
        } 
        else if (CMD_UPGRADE_SYSTEM_DATA.equalsIgnoreCase(command)) {
            var jobClasspath = secondaryArg;
            performSystemUpgradeWithData(jobClasspath);
        } 
        else {
            LogUtil.println("Invalid 2-argument command: " + command);
            printHelpInfo();
        }
    }

    // --- Helper Wrappers to eliminate "Magic Booleans" ---

    private static void performSystemUpgrade() throws Exception {
        // upgradeSystem(upgradeCode, resetConfig, resetAi, resetData, jobClass)
        SystemControl.upgradeSystem(true, false, false, false, null);
    }

    private static void performSystemUpgradeWithConfigReset() throws Exception {
        SystemControl.upgradeSystem(true, true, false, false, null);
    }

    private static void performAiAgentUpgrade() throws Exception {
        SystemControl.upgradeSystem(true, false, true, false, null);
    }

    private static void performSystemUpgradeWithData(String jobClasspath) throws Exception {
        SystemControl.upgradeSystem(true, false, false, false, jobClasspath);
    }

    // --- Help Output ---

    private static void printHelpInfo() {
        var helpMessage = new StringBuilder();
        helpMessage.append("\n================ LEO CDP STARTER HELP ================\n")
                   .append("Usage:\n")
                   .append("  [No Args]                               Start with default Admin Config\n")
                   .append("  [ConfigKey]                             Start with specific Routing Config\n\n")
                   .append("System Management Commands:\n")
                   .append("  ").append(CMD_SETUP_NEW_SYSTEM).append(" [password]   Setup new system\n")
                   .append("  ").append(CMD_UPGRADE_SYSTEM).append("                 Upgrade system core\n")
                   .append("  ").append(CMD_UPGRADE_RESET_CONFIGS).append("      Upgrade & reset configs\n")
                   .append("  ").append(CMD_UPGRADE_AI_AGENT).append("     Upgrade AI agent & reset configs\n")
                   .append("  ").append(CMD_UPGRADE_INDEX_DB).append("          Upgrade database indexes\n")
                   .append("  ").append(CMD_UPGRADE_SYSTEM_DATA).append(" [jobCp]       Upgrade system & run data job\n")
                   .append("======================================================");
        
        LogUtil.println(helpMessage.toString());
    }
}