package leotech.system.model;

/**
 * 
 * for user authorization and data permissions
 * 
 * @author tantrieuf31
 * @since 2022
 */
public final class SystemUserRole {

	/**
	 * the login account can access Unified Analytics Hub to view summary reports
	 */
	public static final int REPORT_VIEWER = 0;
	
	/**
	 * the login account has permissions to view authorized customer data (profiles and segments)
	 */
	public static final int CUSTOMER_DATA_VIEWER = 1;	
	
	/**
	 * the login account can access Unified Analytics Hub and view authorized customer data 
	 */
	public static final int STANDARD_USER = 2;
	
	/**
	 * the login account has permissions to manage authorized customer data (profiles and segments)
	 */
	public static final int CUSTOMER_DATA_EDITOR = 3;

	/**
	 * the login account has access Unified Analytics Hub, manage Journey Data Hub and update authorized customer profiles 
	 */
	public static final int DATA_OPERATOR = 4;

	/**
	 * the login account has full permissions to manage customer data, can manage Data Asset and Data Service
	 */
	public static final int DATA_ADMIN = 5;

	/**
	 * the login account has full of permissions to do data governance and manage login accounts, all data in domain
	 */
	public static final int SUPER_SYSTEM_ADMIN = 6;
	
	/**
	 * the login account has full of permissions to manage platform 
	 */
	public static final int SUPER_PLATFORM_ADMIN = 7;
}
