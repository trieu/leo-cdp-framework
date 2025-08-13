package leotech.cdp.data;

import com.google.gson.JsonArray;

/**
 * Data Pipeline interface
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public interface DataPipeline {
	
	public void processBatchData(String topic, JsonArray dataView) ;

}
