package leotech.cdp.model;

import com.arangodb.model.PersistentIndexOptions;

/**
 * 
 * DbIndexUtil: provide Database Indexing Utility Methods
 * 
 * @author Trieu Nguyen (Thomas)
 * @since 2023
 *
 */
public class DbIndexUtil {

	public final static PersistentIndexOptions createNonUniquePersistentIndex() {
		return createNonUniquePersistentIndex(true);
	}
	
	public final static PersistentIndexOptions createNonUniquePersistentIndex(boolean cacheEnabled) {
		return new PersistentIndexOptions().inBackground(true).unique(false).cacheEnabled(cacheEnabled);
	}
	
	public final static PersistentIndexOptions createUniquePersistentIndex() {
		return createUniquePersistentIndex(false);
	}
	
	public final static PersistentIndexOptions createUniquePersistentIndex(boolean cacheEnabled) {
		return new PersistentIndexOptions().inBackground(true).unique(false).cacheEnabled(cacheEnabled);
	}
}
