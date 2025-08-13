package leotech.system.util.database;

import java.util.Date;
import java.util.Map;
import java.util.zip.CRC32;

import com.arangodb.ArangoDatabase;
import com.arangodb.model.PersistentIndexOptions;

import leotech.cdp.dao.AbstractCdpDatabaseUtil;
import leotech.cdp.model.DbIndexUtil;
import leotech.system.exception.InvalidDataException;
import leotech.system.util.IdGenerator;
import rfx.core.util.StringUtil;

/**
 * @author tantrieuf31
 * 
 * common base class for all models in CDP and CMS
 *
 */
public abstract class PersistentObject implements PersistentArangoObject {
	
	public static final int STATUS_DEAD = -44;
	public static final int STATUS_REMOVED = -4;
	public static final int STATUS_INVALID = -1;
	public static final int STATUS_INACTIVE = 0;
	public static final int STATUS_ACTIVE = 1;
	
	static final Map<String, Long> PARTITION_MAP = ArangoDbUtil.initDbConfigs().getPartitionMap();

	public final static String CDP_COLLECTION_PREFIX = "cdp_";
	
	
	public static String getCdpCollectionName(Class<?> childClass) {
		return CDP_COLLECTION_PREFIX + childClass.getSimpleName().toLowerCase();
	}
	
	protected static final ArangoDatabase getArangoDatabase() {
		return AbstractCdpDatabaseUtil.getCdpDatabase();
	}
	
	protected static PersistentIndexOptions createNonUniquePersistentIndex() {
		return DbIndexUtil.createNonUniquePersistentIndex();
	}
	
	protected static PersistentIndexOptions createPersistentIndex(boolean unique) {
		return unique ? DbIndexUtil.createUniquePersistentIndex() : DbIndexUtil.createNonUniquePersistentIndex();
	}
	
	public static final String createHashedId(String keyHint) {
		return IdGenerator.createHashedId(keyHint);
	}
	
	public static final String createId(String currentId, String keyHint) {
		if(StringUtil.isNotEmpty(currentId)) {
			return currentId;
		}
		return createHashedId(keyHint);
	}
	
	public static final int createPartitionId(String id, Class<?> clazz) {
		CRC32 crc = new CRC32();
        crc.update(id.getBytes());
        long val = crc.getValue();
        String simpleName = clazz.getSimpleName();
		long numPartition = PARTITION_MAP.getOrDefault(simpleName, val);
        if(numPartition > 0) {
        	int p = (int)(val % numPartition);
            return p;
        }
		return 0;
	}
	
	public abstract Date getCreatedAt();
	public abstract void setCreatedAt(Date createdAt);
	
	public abstract Date getUpdatedAt() ;
	public abstract void setUpdatedAt(Date updatedAt);
	public abstract long getMinutesSinceLastUpdate();
	
    /**
     * to calculate the difference between two dates in minutes
     * 
     * @param updatedAt
     * @return diffInMinutes
     */
    protected final long getDifferenceInMinutes(Date updatedAt) {
    	if(updatedAt != null) {
    		Date now = new Date();
            // Calculate the difference in milliseconds
            long diffInMillis = Math.abs(now.getTime() - updatedAt.getTime());
            // Convert milliseconds to minutes
            long diffInMinutes = diffInMillis / (60 * 1000);
    		return (long) Math.floor( diffInMinutes ); 
    	}
    	return 0;
    }
	
	public abstract String buildHashedId() throws IllegalArgumentException;
	
	public abstract String getDocumentUUID();
	
	public static void newIllegalArgumentException(String msg) {
		throw new InvalidDataException(msg);
	}
	
	public static final String getDocumentUUID(final String COLLECTION_NAME, final String id) {
		return COLLECTION_NAME + "/" + id;
	}
}
