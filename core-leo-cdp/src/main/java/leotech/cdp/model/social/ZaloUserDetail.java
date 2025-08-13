package leotech.cdp.model.social;

import com.google.gson.JsonObject;

import leotech.system.util.LogUtil;
import rfx.core.util.StringUtil;

/**
 * Zalo User Detail from Open API of Zalo version 3
 * 
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public class ZaloUserDetail {
	
	private static final String AVATAR_ZALO_USER = "https://cdn-icons-png.flaticon.com/256/906/906382.png";

	public static final String ZALO_USER = "Zalo User";
	
	String userId = null;
	String displayName = null;
	boolean isFollower;
	String lastInteractionDate;
	String avatar240Url = "";
	

	public ZaloUserDetail(String userId) {
		super();
		this.userId = userId;
	}

	public ZaloUserDetail(String user_id, JsonObject jsonObject) {
		// Access data object and its properties
		JsonObject dataObject = jsonObject.getAsJsonObject("data");
		if (dataObject != null) {
			if (dataObject.has("user_id")) {
				this.userId = dataObject.get("user_id").getAsString();
				this.displayName = dataObject.get("display_name").getAsString();
				this.isFollower = dataObject.get("user_is_follower").getAsBoolean();
				this.lastInteractionDate = dataObject.get("user_last_interaction_date").getAsString();

				// Access avatars object and its properties (if needed)
				JsonObject avatarsObject = dataObject.getAsJsonObject("avatars");
				this.avatar240Url = avatarsObject.get("240").getAsString();
			}
		}
		else {
			this.userId = user_id;
			this.displayName = ZALO_USER;
			this.avatar240Url = AVATAR_ZALO_USER;
		}
	}
	
	public boolean isValidVisitor() {
		return StringUtil.isNotEmpty(userId);
	}

	public boolean isUserContact() {
		return StringUtil.isNotEmpty(displayName) && StringUtil.isNotEmpty(userId) && StringUtil.isNotEmpty(avatar240Url);
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public boolean isFollower() {
		return isFollower;
	}

	public void setFollower(boolean isFollower) {
		this.isFollower = isFollower;
	}

	public String getLastInteractionDate() {
		return lastInteractionDate;
	}

	public void setLastInteractionDate(String lastInteractionDate) {
		this.lastInteractionDate = lastInteractionDate;
	}

	public String getAvatar240Url() {
		return avatar240Url;
	}

	public void setAvatar240Url(String avatar240Url) {
		this.avatar240Url = avatar240Url;
	}

	@Override
	public String toString() {
		return LogUtil.toPrettyJson(this);
	}
}
