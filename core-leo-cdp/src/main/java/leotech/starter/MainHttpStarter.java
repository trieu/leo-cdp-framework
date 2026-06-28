package leotech.starter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itfsw.query.builder.support.parser.AbstractArangoQueryRuleParser;
import leotech.system.HttpWorker;
import leotech.system.domain.SystemControl;
import leotech.system.util.LogUtil;
import leotech.system.version.SystemMetaData;

/**
 * The main starter class for the LEO CDP service instance.
 * <p>
 * This class handles CLI arguments to either start the HTTP worker with
 * specific
 * routing configurations (defined in http-routing-configs.json) or execute
 * various system setup and maintenance commands.
 * </p>
 * * @author tantrieuf31
 * 
 * @since 2020
 */
public final class MainHttpStarter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainHttpStarter.class);

    // Standard Commands
    private static final String CMD_HELP = "help";
    private static final String CMD_SETUP_NEW_SYSTEM = "setup-system-with-password";

    // Upgrade Commands
    private static final String CMD_UPGRADE_SYSTEM = "upgrade-system";
    private static final String CMD_UPGRADE_RESET_CONFIGS = "upgrade-system-and-reset-configs";
    private static final String CMD_UPGRADE_AI_AGENT = "upgrade-ai-agent-and-reset-configs";
    private static final String CMD_UPGRADE_SYSTEM_DATA = "upgrade-system-and-data";
    private static final String CMD_UPGRADE_INDEX_DB = "upgrade-index-database";

    /**
     * Private constructor to prevent instantiation of this utility/starter class.
     */
    private MainHttpStarter() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated.");
    }

    /**
     * Main entry point for the application.
     * * @param args Command line arguments passed during application startup.
     */
    public static void main(String[] args) {
        try {
            initializeSystemEnvironment();
            dispatchCommand(args);
        } catch (Exception e) {
            LOGGER.error("Fatal error during system startup: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    /**
     * Initializes core system environment variables, loggers, and timezones
     * before processing any specific commands.
     */
    private static void initializeSystemEnvironment() {
        LogUtil.loadLoggerConfigs();
        SystemMetaData.initTimeZoneGMT();
        AbstractArangoQueryRuleParser.setTimeZone(SystemMetaData.DEFAULT_TIME_ZONE);
    }

    /**
     * Parses the command line arguments and routes execution to the appropriate
     * handler.
     * * @param args The command line arguments array.
     * 
     * @throws Exception if a system configuration or startup process fails.
     */
    private static void dispatchCommand(String[] args) throws Exception {
        int argCount = args.length;

        // Case 0: No arguments provided. Default Startup.
        if (argCount == 0) {
            LogUtil.println("Starting HTTP Worker with default admin config...");
            HttpWorker.start(SystemMetaData.HTTP_ROUTING_CONFIG_ADMIN);
            return;
        }

        final String command = args[0].trim();

        // Case 1: Single Argument Commands (e.g., upgrades, help, custom config)
        if (argCount == 1) {
            handleSingleArgCommand(command);
            return;
        }

        // Case 2: Double Argument Commands (e.g., setup with password, data upgrade
        // with classpath)
        if (argCount == 2) {
            final String secondaryArg = args[1].trim();
            handleDoubleArgCommand(command, secondaryArg);
            return;
        }

        // Case Default: Too many arguments or invalid pattern
        LogUtil.println("Invalid number of arguments provided.");
        printHelpInfo();
    }

    /**
     * Handles logic for commands that require exactly one argument.
     * * @param command The primary command string.
     * 
     * @throws Exception if the executed command fails.
     */
    private static void handleSingleArgCommand(String command) throws Exception {
        // Maintained as if-else chains for Java 11 compatibility
        if (CMD_UPGRADE_SYSTEM.equalsIgnoreCase(command)) {
            performSystemUpgrade();
        } else if (CMD_UPGRADE_RESET_CONFIGS.equalsIgnoreCase(command)) {
            performSystemUpgradeWithConfigReset();
        } else if (CMD_UPGRADE_AI_AGENT.equalsIgnoreCase(command)) {
            performAiAgentUpgrade();
        } else if (CMD_UPGRADE_INDEX_DB.equalsIgnoreCase(command)) {
            SystemControl.upgradeIndexDatabase();
        } else if (command.toLowerCase().contains(CMD_HELP)) {
            printHelpInfo();
        } else {
            // If it doesn't match standard commands, treat it as a custom routing config
            // key
            LogUtil.println("Starting HTTP Worker with config key: " + command);
            HttpWorker.start(command);
        }
    }

    /**
     * Handles logic for commands that require exactly two arguments.
     * * @param command The primary command string.
     * 
     * @param secondaryArg The secondary configuration string or parameter.
     * @throws Exception if the executed command fails.
     */
    private static void handleDoubleArgCommand(String command, String secondaryArg) throws Exception {
        if (CMD_SETUP_NEW_SYSTEM.equalsIgnoreCase(command)) {
            SystemControl.setupNewSystem(secondaryArg, true);
        } else if (CMD_UPGRADE_SYSTEM_DATA.equalsIgnoreCase(command)) {
            performSystemUpgradeWithData(secondaryArg);
        } else {
            LogUtil.println("Invalid 2-argument command: " + command);
            printHelpInfo();
        }
    }

    // ========================================================================
    // Helper Wrappers: Encapsulate complex boolean logic into readable methods
    // ========================================================================

    /**
     * Performs a standard system upgrade without resetting configurations.
     */
    private static void performSystemUpgrade() throws Exception {
        // Signature: upgradeSystem(upgradeCode, resetConfig, resetAi, resetData,
        // jobClass)
        SystemControl.upgradeSystem(true, false, false, false, null);
    }

    /**
     * Performs a system upgrade and forcefully resets configurations.
     */
    private static void performSystemUpgradeWithConfigReset() throws Exception {
        SystemControl.upgradeSystem(true, true, false, false, null);
    }

    /**
     * Performs an AI Agent upgrade and resets AI-related configurations.
     */
    private static void performAiAgentUpgrade() throws Exception {
        SystemControl.upgradeSystem(true, false, true, false, null);
    }

    /**
     * Performs a system upgrade and executes a specific data job.
     * * @param jobClasspath The classpath of the data job to execute.
     */
    private static void performSystemUpgradeWithData(String jobClasspath) throws Exception {
        SystemControl.upgradeSystem(true, false, false, false, jobClasspath);
    }

    // ========================================================================
    // Output Formatting
    // ========================================================================

    /**
     * Prints the standard CLI help manual to standard output.
     */
    private static void printHelpInfo() {
        final String newLine = System.lineSeparator();
        StringBuilder helpMessage = new StringBuilder();

        helpMessage.append(newLine).append("================ LEO CDP STARTER HELP ================").append(newLine)
                .append("Usage:").append(newLine)
                .append("  [No Args]                               Start with default Admin Config").append(newLine)
                .append("  [ConfigKey]                             Start with specific Routing Config").append(newLine)
                .append(newLine)
                .append("System Management Commands:").append(newLine)
                .append("  ").append(CMD_SETUP_NEW_SYSTEM).append(" [password]   Setup new system").append(newLine)
                .append("  ").append(CMD_UPGRADE_SYSTEM).append("                 Upgrade system core").append(newLine)
                .append("  ").append(CMD_UPGRADE_RESET_CONFIGS).append("      Upgrade & reset configs").append(newLine)
                .append("  ").append(CMD_UPGRADE_AI_AGENT).append("     Upgrade AI agent & reset configs")
                .append(newLine)
                .append("  ").append(CMD_UPGRADE_INDEX_DB).append("          Upgrade database indexes").append(newLine)
                .append("  ").append(CMD_UPGRADE_SYSTEM_DATA).append(" [jobCp]       Upgrade system & run data job")
                .append(newLine)
                .append("======================================================");

        LogUtil.println(helpMessage.toString());
    }
}