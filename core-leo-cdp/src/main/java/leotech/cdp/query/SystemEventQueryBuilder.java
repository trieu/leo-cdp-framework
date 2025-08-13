package leotech.cdp.query;

import leotech.cdp.query.filters.SystemEventFilter;
import leotech.system.model.SystemEvent;
import rfx.core.util.StringUtil;

/**
 * System Event Query Builder
 * 
 * @author tantrieuf31
 * @since 2023
 *
 */
public class SystemEventQueryBuilder {

	/**
	 * @param filter
	 * @param countingTotal
	 * @return the AQL from SystemEventFilter
	 */
	public static String buildAqlFromSystemEventFilter(SystemEventFilter filter, boolean countingTotal) {
		StringBuilder aql = new StringBuilder();
		
		String loginUsername = filter.getLoginUsername();
		String objectName = filter.getObjectName();
		String objectId = filter.getObjectId();
		String action = filter.getAction();
		String accessIp = filter.getAccessIp();
		
		if(countingTotal) {
			aql.append(" RETURN LENGTH( ");
		}
		
		// set collection to loop
		aql.append("FOR e in ").append(SystemEvent.COLLECTION_NAME);
		
		// add filter loginUsername
		if(StringUtil.isNotEmpty(loginUsername)) {
			aql.append(" FILTER e.loginUsername == \"").append(loginUsername).append("\" ");
			
			if(StringUtil.isNotEmpty(objectName)) {
				aql.append(" AND e.objectName == \"").append(objectName).append("\" ");
			}
			if(StringUtil.isNotEmpty(objectId)) {
				aql.append(" AND e.objectId == \"").append(objectId).append("\" ");
			}
			if(StringUtil.isNotEmpty(action)) {
				aql.append(" AND e.action == \"").append(action).append("\" ");
			}
			if(StringUtil.isNotEmpty(accessIp)) {
				aql.append(" AND e.accessIp == \"").append(accessIp).append("\" ");
			}
		}
		else if(StringUtil.isNotEmpty(objectName)) {
			aql.append("FILTER e.objectName == \"").append(objectName).append("\" ");
			
			if(StringUtil.isNotEmpty(objectId)) {
				aql.append(" AND e.objectId == \"").append(objectId).append("\" ");
			}
			if(StringUtil.isNotEmpty(action)) {
				aql.append(" AND e.action == \"").append(action).append("\" ");
			}
			if(StringUtil.isNotEmpty(accessIp)) {
				aql.append(" AND e.accessIp == \"").append(accessIp).append("\" ");
			}
		}
		
		// total result
		if(countingTotal) {
			aql.append(" RETURN e._key ");
		} 
		else {
			// pagination
			aql.append(" SORT e.createdAt ").append(filter.getSortDirection());
			aql.append(" LIMIT @startIndex, @numberResult ");
			aql.append(" RETURN e ");
		}
		
		if(countingTotal) {
			aql.append(" ) ");
		}
		
		System.out.println(aql);
		return aql.toString();
	}

}
