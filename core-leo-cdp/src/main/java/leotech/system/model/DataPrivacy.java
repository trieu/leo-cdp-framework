package leotech.system.model;

public class DataPrivacy {

	// for viewer, operator and admin
	public static final int PRIVATE = -1;
	public static final int PUBLIC = 0;
	public static final int PROTECTED_WITH_PASSWORD = 1;
	public static final int ANYONE_WITH_LINK = 2;
	
	// only be deleted by SuperAdmin (Root user)
	public static final int SYSTEM_DOCUMENT = 3;

}
