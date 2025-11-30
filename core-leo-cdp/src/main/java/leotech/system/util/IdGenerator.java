package leotech.system.util;

import java.util.UUID;

import com.devskiller.friendly_id.FriendlyId;

import leotech.system.common.SecuredHttpDataHandler;
import rfx.core.util.RandomUtil;

/**
 * The ID generator for all persistent objects in CDP
 * 
 * @author tantrieuf31
 * @since 2019
 *
 */
public final class IdGenerator {
	
	private static final int RAMDOM_LIMIT = 9000000;

	/**
	 * create friendly ID token from keyHint
	 * 
	 * @param keyHint
	 * @return
	 */
	public final static String createHashedId(String keyHint) {
		return FriendlyId.toFriendlyId(UUID.nameUUIDFromBytes(keyHint.getBytes()));
	}
	
	/**
	 * for exporting or downloading data from CDP admin
	 * 
	 * @param objectId
	 * @param systemUserId
	 * @return dataAccessKey
	 */
	public static String generateDataAccessKey(String objectId, String systemUserId) {
		String keyHint = objectId + System.currentTimeMillis() + "#" + RandomUtil.getRandom(RAMDOM_LIMIT);
		String dataAccessKey = FriendlyId.toFriendlyId(UUID.nameUUIDFromBytes(keyHint.getBytes()));
		SecuredHttpDataHandler.setDataAccessKeyForSystemUser(dataAccessKey, systemUserId);
		return dataAccessKey;
	}
}