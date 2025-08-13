package leotech.cdp.model.asset;

public class AssetItemStatus {
    
    /**
     * Incomplete post viewable by anyone with proper user role. (draft)
     */
    public static final int DRAFTED = 0;    
    
    /** 
     * Revisions that CMS saves automatically while you are editing. (auto-draft)
     */
    public static final int AUTO_DRAFTED = 1;
    
    /** 
     * Scheduled to be published in a future date. (future)
     */
    public static final int SCHEDULED = 2;    
    
    /** 
     * Awaiting a user with the publish_posts capability (typically a user assigned the Editor role) to publish. (pending)
     */
    public static final int PENDING = 3;
    
    /** 
     * Viewable by everyone. (publish)
     */
    public static final int PUBLISHED = 9;
    
    /** 
     * Posts in the Trash are assigned the trash status. (trash)
     */
    public static final int DELETED = 4;
    
    /** 
     * Viewable only to CMS users at Administrator level. (private)
     */
    public static final int PRIVATE = 8;
    
    /** 
     * Used with a child post (such as Attachments and Revisions) to determine the actual status from the parent post. (inherit)
     */
    public static final int INHERITED = 10;
    
    
}
