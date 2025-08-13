package leotech.cdp.model.asset;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.annotations.Expose;

import leotech.cdp.model.marketing.TargetMediaUnit;
import rfx.core.util.StringUtil;

/**
 * 
 * the measurable data model for public asset item, with interaction data from audience
 * 
 * @author tantrieuf31
 * @since 2018
 *
 */
public abstract class MeasurableItem extends AssetItem {
	
	@Expose
	protected String fullUrl;

	@Expose
	protected long viewCount = 0;
	
	@Expose
	protected long clickCount = 0;
	
	@Expose
	protected long transactionCount = 0;
	
	@Expose
	protected long shareCount = 0;
	
	@Expose
	protected long feedbackCount = 0;
	
	@Expose
	protected Map<String, ReactionReportUnit> reactionReportUnits = new HashMap<>();

	protected MeasurableItem() {
		// for GSON and JSON decode from ArangoDB
	}
	
	public static List<MeasurableItem> sortBy(List<MeasurableItem> list) {
		Collections.sort(list, new Comparator<MeasurableItem>() {
			@Override
			public int compare(MeasurableItem o1, MeasurableItem o2) {
				if (o1.getModificationTime() > o2.getModificationTime()) {
					return 1;
				} else if (o1.getModificationTime() < o2.getModificationTime()) {
					return -1;
				}
				return 0;
			}
		});
		return list;
	}

	public long getViewCount() {
		return viewCount;
	}

	public void setViewCount(long viewCount) {
		this.viewCount = viewCount;
	}

	public long getClickCount() {
		return clickCount;
	}

	public void setClickCount(long clickCount) {
		this.clickCount = clickCount;
	}
	
	public long getTransactionCount() {
		return transactionCount;
	}

	public void setTransactionCount(long transactionCount) {
		this.transactionCount = transactionCount;
	}

	public long getShareCount() {
		return shareCount;
	}

	public void setShareCount(long shareCount) {
		this.shareCount = shareCount;
	}

	public long getFeedbackCount() {
		return feedbackCount;
	}

	public void setFeedbackCount(long feedbackCount) {
		this.feedbackCount = feedbackCount;
	}
	
	public Map<String, ReactionReportUnit> getFeedbackReactions() {
		return reactionReportUnits;
	}

	public void setFeedbackReactions(Map<String, ReactionReportUnit> feedbackReactions) {
		this.reactionReportUnits = feedbackReactions;
	}
	
	public String getFullUrl() {
		return fullUrl;
	}

	public void setFullUrl(String fullUrl) {
		this.fullUrl = fullUrl;
	}
	
	public TargetMediaUnit createTargetMediaUnit() {
		if(StringUtil.isValidUrl(fullUrl)) {
			String landingPageUrl = StringUtil.safeString(this.fullUrl).trim();
			if(StringUtil.isNotEmpty(landingPageUrl)) {
				TargetMediaUnit targetMediaUnit = null;
				if(this.isContent()) {
					targetMediaUnit = TargetMediaUnit.fromContent(this.id, headlineImageUrl, headlineVideoUrl, "" , "", landingPageUrl, this.title);
					
				}
				else if(this.isProduct()) {
					targetMediaUnit = TargetMediaUnit.fromProduct(this.id, headlineImageUrl, headlineVideoUrl, "" , "", landingPageUrl, this.title);
					
				}
				if(targetMediaUnit != null) {
					targetMediaUnit.setCustomData(this.customData);
					return targetMediaUnit;
				}
			} 
		}
		throw new IllegalArgumentException(" landingPageUrl is empty");
	}
	
}
