package leotech.cdp.domain;

import leotech.cdp.dao.TargetMediaUnitDaoUtil;
import leotech.cdp.model.marketing.TargetMediaUnit;

/**
 * target media unit management
 * 
 * @author tantrieuf31
 * @since 2021
 *
 */
public class TargetMediaUnitManagement {

	static final String OBSERVER_ID = "leo-click-redirect";

	/**
	 * @param id
	 * @return
	 */
	public static TargetMediaUnit getById(String id) {
		TargetMediaUnit media = TargetMediaUnitDaoUtil.getTargetMediaUnitById(id);
		if(media != null) {			
			return media;
		} else {
			System.err.println("TargetMediaUnit is null " + id);
		}		
		return null;
	}


	
	/**
	 * @param m
	 * @return
	 */
	public static String save(TargetMediaUnit m) {
		return TargetMediaUnitDaoUtil.save(m);
	}
}
