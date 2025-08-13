package leotech.cdp.model.analytics;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import leotech.system.util.UrlUtil;
import rfx.core.util.StringUtil;

/**
 * The report object for touchpoint flow
 * 
 * @author tantrieuf31
 * @since 2022
 */
public class TouchpointFlowReport extends ReportProfileEvent {

	String observerId;
	String refTouchpointId, refTouchpointName, refTouchpointUrl;
	String srcTouchpointId, srcTouchpointName, srcTouchpointUrl;

	public TouchpointFlowReport() {

	}

	public String getRefTouchpointId() {
		return refTouchpointId;
	}

	public void setRefTouchpointId(String refTouchpointId) {
		this.refTouchpointId = refTouchpointId;
	}

	public String getRefTouchpointName() {
		return refTouchpointName;
	}

	public void setRefTouchpointName(String refTouchpointName) {
		this.refTouchpointName = refTouchpointName;
	}

	public String getSrcTouchpointId() {
		return srcTouchpointId;
	}

	public void setSrcTouchpointId(String srcTouchpointId) {
		this.srcTouchpointId = srcTouchpointId;
	}

	public String getSrcTouchpointName() {
		return srcTouchpointName;
	}

	public void setSrcTouchpointName(String srcTouchpointName) {
		this.srcTouchpointName = srcTouchpointName;
	}

	public String getRefTouchpointUrl() {
		return refTouchpointUrl;
	}

	public String getRefTouchpointDomain() {
		return UrlUtil.getHostName(refTouchpointUrl);
	}

	public String getSrcTouchpointDomain() {
		return UrlUtil.getHostName(srcTouchpointUrl);
	}

	public void setRefTouchpointUrl(String refTouchpointUrl) {
		this.refTouchpointUrl = refTouchpointUrl;
	}

	public String getSrcTouchpointUrl() {
		return srcTouchpointUrl;
	}

	public void setSrcTouchpointUrl(String srcTouchpointUrl) {
		this.srcTouchpointUrl = srcTouchpointUrl;
	}

	public String getObserverId() {
		return observerId;
	}

	public void setObserverId(String observerId) {
		this.observerId = observerId;
	}

	public boolean isInboundFlow() {
		return StringUtil.isNotEmpty(this.srcTouchpointId);
	}

	public boolean isOutboundFlow() {
		return StringUtil.isNotEmpty(this.refTouchpointId);
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
	}
}
