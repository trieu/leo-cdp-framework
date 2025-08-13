package leotech.cdp.domain.processor;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import leotech.cdp.dao.ProfileDaoUtil;
import leotech.cdp.domain.ProfileDataManagement;
import leotech.cdp.domain.schema.FunnelMetaData;
import leotech.cdp.handler.HttpParamKey;
import leotech.cdp.model.customer.ProfileGenderCode;
import leotech.cdp.model.customer.ProfileIdentity;
import leotech.cdp.model.customer.ProfileModelUtil;
import leotech.cdp.model.customer.ProfileSingleView;
import rfx.core.util.StringUtil;

/**
 * Profile Data Import from SQL database
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public final class SqlDatabaseImportProcessor {
	
	static Logger logger = LoggerFactory.getLogger(SqlDatabaseImportProcessor.class);

	
	static Map<String,Field> profileFields = ProfileModelUtil.getAutoSetDataByWebFormFields();
	
	String sqlTemplate;
	Map<String, String> fieldMap;
	int c = 1;
	
	public SqlDatabaseImportProcessor(String sqlTemplate, Map<String, String> fieldMap) {
		super();
		this.sqlTemplate = sqlTemplate;
		this.fieldMap = fieldMap;
	}

	public void importProfileFromSqlDatabase(String dataTable, String dbUrl, String username, String password, int batchSize, int maxResult) {
		Jdbi jdbi = Jdbi.create(dbUrl, username, password);
		jdbi.useHandle(handle -> {
			connectAndProcess(dataTable, batchSize, maxResult, handle);
		});
	}

	private void connectAndProcess(String dataTable, int batchSize, int maxResult, Handle handle) {
		int offset = 0;
		
		String sql = null;
		int count = 1;
		int max = maxResult;		
		
		while(count > 0 && max > 0) {
			if(sql == null) {
				sql = buildSql(dataTable, offset, batchSize);
			}				
			count = runSql(handle, sql);
			max -= count;
			offset += batchSize;
			
			if(max < 0 || count == 0) {
				break;
			}				
			
			if(max < batchSize) {
				sql = buildSql(dataTable, offset, max);
			}
			else {
				sql = buildSql(dataTable, offset, batchSize);	
			}
		}
	}

	private String buildSql(String dataTable, int offset, int limit) {
		String sql = sqlTemplate.replace("$dataTable", dataTable).replace("$limit", String.valueOf(limit)).replace("$offset", String.valueOf(offset));
		logger.info("SqlImporter.buildSql: " + sql);
		return sql;
	}

	private int runSql(Handle handle, String sql) {
		Query query = handle.createQuery(sql);
		List<Map<String, Object>> results = query.mapToMap().list();
		for (Map<String, Object> row : results) {
			processRow(row);
		}
		return results.size();
	}
	
	private void processRow(Map<String, Object> row) {
		Map<String, String> localFieldMap = new HashMap<String, String>(fieldMap);
		
		String primaryPhone = StringUtil.safeString(row.get(localFieldMap.get("primaryPhone")), "");
		localFieldMap.remove("primaryPhone");		
		
		String primaryEmail = StringUtil.safeString(row.get(localFieldMap.get("primaryEmail")), "");
		localFieldMap.remove("primaryEmail");		
		
		String crmRefId = StringUtil.safeString(row.get(localFieldMap.get("crmRefId")), "");
		localFieldMap.remove("crmRefId");		
		
		String funnelStage =  StringUtil.safeString(row.get(localFieldMap.get(HttpParamKey.FUNNEL_STAGE)), FunnelMetaData.STAGE_LEAD);
		
		ProfileIdentity profileIdentity = new ProfileIdentity(crmRefId, primaryEmail, primaryPhone);
		ProfileSingleView finalProfile = ProfileDataManagement.searchAndMergeAsUniqueProfile(profileIdentity, true, funnelStage);
		
		if(finalProfile != null) {
			String genderStr =StringUtil.safeString(row.get(localFieldMap.get("gender")), ProfileGenderCode.GENDER_UNKNOWN);
			localFieldMap.remove("gender");
			
			finalProfile.setGender(genderStr);
			finalProfile.setEmail(primaryEmail);
			finalProfile.setPhone(primaryPhone);
			finalProfile.setCrmRefId(crmRefId);
			finalProfile.addDataLabel(ProfileDataManagement.FROM_CDP_API);
		
			localFieldMap.forEach((profileFieldName, importedFieldName)->{
				Object value = row.get(importedFieldName);
				ProfileModelUtil.updateProfileWithAttribute(finalProfile, profileFields, profileFieldName, value);
			});
			
			// save profile into database
			if(finalProfile.isInsertingData()) {
				ProfileDaoUtil.insertProfile(finalProfile, null);
			}
			else {
				ProfileDaoUtil.saveProfile(finalProfile);	
			}
		}
		
		int index = c++;
		logger.info(index + "=> FullName " + finalProfile.getFullName() + " primaryEmail " + finalProfile.getPrimaryEmail() + " primaryPhone " + finalProfile.getPrimaryPhone());
	}


}
