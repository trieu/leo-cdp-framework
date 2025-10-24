package leotech.cdp.model.customer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arangodb.ArangoCollection;
import com.arangodb.model.PersistentIndexOptions;

import leotech.cdp.domain.TouchpointHubManagement;
import leotech.cdp.domain.schema.CustomerFunnel;
import leotech.cdp.domain.schema.FunnelMetaData;
import leotech.cdp.domain.scoring.DataQualityScoreUtil;
import leotech.cdp.model.AutoSetData;
import leotech.cdp.model.DbIndexUtil;
import leotech.cdp.model.ExposeInSegmentList;
import leotech.cdp.model.ProfileMetaDataField;
import leotech.cdp.model.analytics.ScoreCX;
import leotech.cdp.model.journey.DataFlowStage;
import leotech.cdp.model.journey.JourneyMapRefKey;
import leotech.cdp.model.journey.TouchpointHub;
import leotech.system.domain.SystemConfigsManagement;
import leotech.system.model.AttributeMetaData;
import leotech.system.util.UrlUtil;
import leotech.system.util.XssFilterUtil;
import leotech.system.version.SystemMetaData;
import rfx.core.util.StringPool;
import rfx.core.util.StringUtil;

/**
 * 
 * ProfileModelUtil
 * 
 * @author Trieu Nguyen (Thomas)
 * @since 2020
 *
 */
public final class ProfileModelUtil {

	private static final String BOOLEAN = "boolean";

	private static final String DOUBLE = "double";

	private static final String INT = "int";

	private static final String JAVA_LANG_INTEGER = "java.lang.Integer";

	private static final String JAVA_LANG_BOOLEAN = "java.lang.Boolean";

	private static final String JAVA_LANG_DOUBLE = "java.lang.Double";

	private static final String JAVA_UTIL_DATE = "java.util.Date";

	private static final String JAVA_LANG_STRING = "java.lang.String";

	private static final String JAVA_UTIL_MAP = "java.util.Map";

	private static final String JAVA_UTIL_SET = "java.util.Set";

	static Logger logger = LoggerFactory.getLogger(ProfileModelUtil.class);

	static final String EXT = "ext";
	static final String YYYY_MM_DD = "yyyy-MM-dd";
	static final String DD_MM_YYYY = "dd/MM/yyyy";
	static final String CSV_FIELDS = "email,phone,fn,ln,dob,ct,st,zip,country,value,cac,trv,loyalty,prospect,engage,nps,csat,ces,cfs,leopid,leovid,crmId";

	static List<String> exportedProfileIdentities = null;
	static List<String> exposedFieldNames = null;
	static Map<String, Field> autoSetDataByWebFormFields = null;
	static volatile Map<String, AttributeMetaData> profileMetaDataMap = null;
	static volatile int maxTotalDataQualityScore = 0;
	static Set<String> searchHosts = new HashSet<String>(Arrays.asList("google.com", "yahoo.com", "bing.com"));
	static Set<String> socialHosts = new HashSet<String>(
			Arrays.asList("facebook.com", "linkedin.com", "youtube.com", "tiktok.com", "instagram.com"));

	public static void init() {
		exposedFieldNames = getExposedFieldNamesInSegmentList();
		autoSetDataByWebFormFields = getAutoSetDataByWebFormFields();
		initAndLoadProfileMetaData();
	}

	static {
		init();
	}

	public final static int getMaxTotalDataQualityScore() {
		return maxTotalDataQualityScore;
	}

	public static int checkProfileType(String email, String phone) {
		int type = ProfileType.ANONYMOUS_VISITOR;
		if (StringUtil.isNotEmpty(email) || StringUtil.isNotEmpty(phone)) {
			type = ProfileType.CUSTOMER_CONTACT;
		}
		return type;
	}

	/**
	 * 
	 * @return Map<String,AttributeMetaData>
	 */
	public final synchronized static Map<String, AttributeMetaData> initAndLoadProfileMetaData() {
		if (profileMetaDataMap == null) {
			Class<Profile> clazz = Profile.class;
			Field[] fields = FieldUtils.getAllFields(clazz);

			profileMetaDataMap = new HashMap<String, AttributeMetaData>(fields.length);
			maxTotalDataQualityScore = 0;

			for (Field field : fields) {
				String fieldName = field.getName();

				String methodName;
				String fieldType = field.getType().getName();
				if (fieldType.equals(BOOLEAN) || fieldType.equals(JAVA_LANG_BOOLEAN)) {
					methodName = "is" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
				} else {
					methodName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
				}

				int dqScore = 0;
				String label = "";
				String invalidWhen = "";
				boolean synchronizable = false;
				boolean identityResolution = false;
				if (field.isAnnotationPresent(ProfileMetaDataField.class)) {
					ProfileMetaDataField dataField = field.getAnnotation(ProfileMetaDataField.class);
					if (dataField != null) {
						field.setAccessible(true); // should work on private fields
						dqScore = dataField.dataQualityScore();
						label = dataField.label();
						invalidWhen = dataField.invalidWhenEqual();
						synchronizable = dataField.synchronizable();
						identityResolution = dataField.identityResolutionKey();
					}
				}
				// logger.info(fieldName + " dataQualityScore: " + dqScore);

				if (!Modifier.isStatic(field.getModifiers())) {
					boolean basicField = fieldType.startsWith("java.") || fieldType.equals(INT)
							|| fieldType.equals(DOUBLE) || fieldType.equals("long") || fieldType.equals("float");
					AttributeMetaData fieldConfig = new AttributeMetaData(fieldName, methodName, label,
							identityResolution, basicField, true, basicField, fieldType, dqScore, invalidWhen,
							synchronizable);
					profileMetaDataMap.put(fieldName, fieldConfig);
					maxTotalDataQualityScore += dqScore;
				}
			}
		}
		return profileMetaDataMap;
	}

	/**
	 * @return exposedFieldNames (List)
	 */
	public final static List<String> getExposedFieldNamesInSegmentList() {
		if (exposedFieldNames == null) {
			Class<Profile> clazz = Profile.class;
			Field[] fields = FieldUtils.getAllFields(clazz);
			exposedFieldNames = new ArrayList<String>(fields.length + 1);
			exposedFieldNames.add("_key");

			for (Field field : fields) {
				String name = field.getName();

				ExposeInSegmentList segmentation = field.getAnnotation(ExposeInSegmentList.class);
				boolean isOkToExport = !Modifier.isStatic(field.getModifiers()) && segmentation != null;
				if (isOkToExport) {
					exposedFieldNames.add(name);
				}
			}
		}
		return exposedFieldNames;
	}

	/**
	 * @return
	 */
	public final static List<String> getExportedProfileIdentitiesForSegment() {
		if (exportedProfileIdentities == null) {
			Class<ProfileIdentity> clazz = ProfileIdentity.class;
			Field[] fields = FieldUtils.getAllFields(clazz);
			exportedProfileIdentities = new ArrayList<String>(fields.length + 1);
			exportedProfileIdentities.add("_key");

			for (Field field : fields) {
				String name = field.getName();
				if (!Modifier.isStatic(field.getModifiers())) {
					exportedProfileIdentities.add(name);
				}
			}
		}
		return exportedProfileIdentities;
	}

	/**
	 * @return
	 */
	public final static Map<String, Field> getAutoSetDataByWebFormFields() {
		if (autoSetDataByWebFormFields == null) {
			Class<Profile> clazz = Profile.class;
			Field[] fields = FieldUtils.getAllFields(clazz);
			autoSetDataByWebFormFields = new HashMap<>(fields.length);
			for (Field field : fields) {
				String name = field.getName();
				AutoSetData metadata = field.getAnnotation(AutoSetData.class);
				if (metadata != null) {
					autoSetDataByWebFormFields.put(name, field);
				}
			}
		}
		return autoSetDataByWebFormFields;
	}

	/**
	 * @param aCollection (ArangoCollection)
	 */
	final static void checkAndBuild(ArangoCollection aCollection) {
		if (aCollection == null) {
			throw new IllegalArgumentException("dbCol for profile is null");
		}

		// ensure indexing key fields for fast lookup
		PersistentIndexOptions defaultIndexOptions = DbIndexUtil.createNonUniquePersistentIndex();
		PersistentIndexOptions nonUniquePersistentIndex = DbIndexUtil.createNonUniquePersistentIndex(true);
		
		aCollection.ensurePersistentIndex(Arrays.asList("crmRefId"), defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("primaryEmail"), defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("secondaryEmails[*]"),
				defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("primaryPhone"), defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("secondaryPhones[*]"),
				defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("identities[*]"), defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("identitiesAsStr"),
				defaultIndexOptions);

		aCollection.ensurePersistentIndex(Arrays.asList("type"), defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("status"), defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("createdAt"), defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("updatedAt"), defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("status", "updatedAt"),defaultIndexOptions);
		
		// dashboard
		aCollection.ensurePersistentIndex(Arrays.asList("funnelStage", "status", "updatedAt"),nonUniquePersistentIndex);
		aCollection.ensurePersistentIndex(Arrays.asList("primaryEmail", "primaryPhone", "status","type"),nonUniquePersistentIndex);
		
		// to check permission
		aCollection.ensurePersistentIndex(Arrays.asList("authorizedEditors[*]"),
				defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("authorizedViewers[*]"),
				defaultIndexOptions);

		// journey data indexing
		aCollection.ensurePersistentIndex(Arrays.asList("topEngagedTouchpointIds[*]"),
				defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("funnelStage"), defaultIndexOptions);

		// behavioral event indexing
		aCollection.ensurePersistentIndex(Arrays.asList("behavioralEvents[*]"),defaultIndexOptions);

		// financial event indexing
		aCollection.ensurePersistentIndex(Arrays.asList("paymentEvents[*]"),defaultIndexOptions);

		// source of data indexing
		aCollection.ensurePersistentIndex(Arrays.asList("lastChannelId"), defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("lastSeenIp"), defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("lastTouchpointId"),
				defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("lastTouchpoint.name"),
				defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("lastTouchpoint.url"),
				defaultIndexOptions);

		// taxonomy for categorization indexing
		aCollection.ensurePersistentIndex(Arrays.asList("personaUri"), defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("dataLabels[*]"), defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("dataLabelsAsStr"),
				defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("inAccounts[*].id"),
				defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("inAccountsAsStr"),
				defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("inSegments[*].id"),
				defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("inSegmentsAsStr"),
				defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("inCampaigns[*].id"),
				defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("inCampaignsAsStr"),
				defaultIndexOptions);

		// FIXME
		aCollection.ensurePersistentIndex(Arrays.asList("inJourneyMaps[*].id", "inJourneyMaps[*].type",
				"inJourneyMaps[*].indexScore", "updatedAt", "status"), defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("inJourneyMaps[*].type"),
				defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("inJourneyMaps[*].id"),
				defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("inJourneyMaps[*].indexScore"),
				defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("inJourneyMapsAsStr"),
				defaultIndexOptions);

		// marketing metadata indexing
		aCollection.ensurePersistentIndex(Arrays.asList("solutionsForCustomer[*]"),
				defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("mediaChannels[*]"),
				defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("contentKeywords[*]"),
				defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("productKeywords[*]"),
				defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("nextBestActions[*]"),
				defaultIndexOptions);

		// e-commerce/retail product item indexing
		aCollection.ensurePersistentIndex(Arrays.asList("purchasedItemIds[*]"),
				defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("shoppingItemIds[*]"),
				defaultIndexOptions);

		// business data indexing
		aCollection.ensurePersistentIndex(Arrays.asList("businessIndustries[*]"),
				defaultIndexOptions);

		// scoring data
		aCollection.ensurePersistentIndex(Arrays.asList("journeyScore"), defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("totalLoyaltyScore"),
				defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("totalCreditScore"),
				defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("totalProspectScore"),
				defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("totalLeadScore"),
				defaultIndexOptions);

		aCollection.ensurePersistentIndex(Arrays.asList("dataQualityScore"),
				defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("totalEngagementScore"),
				defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("totalCAC"), defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("totalCLV"), defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("totalTransactionValue"),
				defaultIndexOptions);

		aCollection.ensurePersistentIndex(Arrays.asList("totalCFS"), defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("positiveCFS"), defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("neutralCFS"), defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("negativeCFS"), defaultIndexOptions);

		aCollection.ensurePersistentIndex(Arrays.asList("totalCES"), defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("positiveCES"), defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("neutralCES"), defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("negativeCES"), defaultIndexOptions);

		aCollection.ensurePersistentIndex(Arrays.asList("totalCSAT"), defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("positiveCSAT"), defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("neutralCSAT"), defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("negativeCSAT"), defaultIndexOptions);

		aCollection.ensurePersistentIndex(Arrays.asList("totalNPS"), defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("positiveNPS"), defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("neutralNPS"), defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("negativeNPS"), defaultIndexOptions);

		aCollection.ensurePersistentIndex(Arrays.asList("rfeScore"), defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("rfmScore"), defaultIndexOptions);
		aCollection.ensurePersistentIndex(Arrays.asList("churnScore"), defaultIndexOptions);
	}

	/**
	 * classify funnel stage of a profile in data funnel (visitor, lead, prospect,
	 * customer, engaged, happy or unhappy)
	 */
	final static int classifyFunnelStageIndex(AbstractProfile p, DataFlowStage updateFunnelStage, JourneyMapRefKey journeyRef, ScoreCX scoreCX) {
		// logger.info("==> classifyFunnelStageIndex");
		// logger.info(updateFunnelStage);
		// logger.info(journeyRef);

		int updateFunnelIndex = updateFunnelStage.getOrderIndex();
		String journeyId = journeyRef.getId();

		int funnelIndex = journeyRef.getFunnelIndex() >= updateFunnelIndex ? journeyRef.getFunnelIndex()
				: updateFunnelIndex;

		// 1 STAGE_VISITOR
		if (funnelIndex == 1) {
			if (p.journeyScore < 10) {
				p.funnelStage = FunnelMetaData.STAGE_NEW_VISITOR;
				p.journeyScore = 10;
			}
			p.updateFunnelStageTimeline(journeyId, CustomerFunnel.NEW_VISITOR);
		}

		// 2 STAGE_RETURNING_VISITOR
		boolean checkReturningVisitor = p.dataQualityScore > 0 && funnelIndex >= 1 && p.totalLeadScore > 2;
		if (checkReturningVisitor) {
			funnelIndex = 2;
			if (p.journeyScore < 20) {
				p.funnelStage = FunnelMetaData.STAGE_RETURNING_VISITOR;
				p.journeyScore = 20;
			}
			p.updateFunnelStageTimeline(journeyId, CustomerFunnel.RETURNING_VISITOR);
		}

		boolean hasContact = p.hasContactData();
		// update type
		if (p.type == 0 && hasContact) {
			p.type = ProfileType.CUSTOMER_CONTACT;
		}

		// 3 STAGE_LEAD
		if (hasContact) {
			funnelIndex = 3;
			p.updateFunnelStageTimeline(journeyId, CustomerFunnel.NEW_VISITOR);
			p.updateFunnelStageTimeline(journeyId, CustomerFunnel.RETURNING_VISITOR);
			p.updateFunnelStageTimeline(journeyId, CustomerFunnel.LEAD);
			if (p.journeyScore < 30) {
				p.journeyScore = 30;
				p.funnelStage = FunnelMetaData.STAGE_LEAD;
			}
		}

		// 4 STAGE_PROSPECT
		boolean checkProspect = hasContact && p.totalProspectScore > 0 && funnelIndex >= 3;
		if (checkProspect) {
			funnelIndex = 4;
			p.updateFunnelStageTimeline(journeyId, CustomerFunnel.PROSPECT);
			if (p.journeyScore < 40) {
				p.journeyScore = 40;
				p.funnelStage = FunnelMetaData.STAGE_PROSPECT;
			}
		}

		// 5 STAGE_NEW_CUSTOMER
		boolean hasTransactionalData = p.hasTransactionalData(journeyId);
		boolean checkCustomer = hasContact && hasTransactionalData && funnelIndex >= 4;
		if (checkCustomer) {
			funnelIndex = 5;
			p.updateFunnelStageTimeline(journeyId, CustomerFunnel.NEW_CUSTOMER);
			if (p.journeyScore < 50) {
				p.journeyScore = 50;
				p.funnelStage = FunnelMetaData.STAGE_NEW_CUSTOMER;
			}
		}

		// 6 STAGE_ENGAGED_CUSTOMER
		boolean hasManyTransactions = p.hasManyTransactions(journeyId);
		boolean checkEngagedCustomer = hasContact && hasManyTransactions && funnelIndex >= 5;
		if (checkEngagedCustomer) {
			funnelIndex = 6;
			p.updateFunnelStageTimeline(journeyId, CustomerFunnel.ENGAGED_CUSTOMER);
			if (p.journeyScore < 60) {
				p.journeyScore = 60;
				p.funnelStage = FunnelMetaData.STAGE_ENGAGED_CUSTOMER;
			}
		}

		if (hasContact && scoreCX != null && funnelIndex >= 5) {
			boolean isHappy = scoreCX.isHappy();
			// 7 STAGE_HAPPY_CUSTOMER
			if (isHappy) {
				p.updateFunnelStageTimeline(journeyId, CustomerFunnel.HAPPY_CUSTOMER);
				if (p.journeyScore < 80) {
					p.journeyScore = funnelIndex == 6 ? 77 : 70;
					p.funnelStage = FunnelMetaData.STAGE_HAPPY_CUSTOMER;
				}
				funnelIndex = 7;
			}
			// 9 STAGE_UNHAPPY_CUSTOMER
			else {
				p.updateFunnelStageTimeline(journeyId, CustomerFunnel.UNHAPPY_CUSTOMER);
				if (p.journeyScore < 99) {
					p.journeyScore = funnelIndex == 6 ? 99 : 90;
					p.funnelStage = FunnelMetaData.STAGE_UNHAPPY_CUSTOMER;
				}
				funnelIndex = 9;
			}
		}

		// 10 STAGE_TERMINATED_CUSTOMER
		if (hasContact && funnelIndex == 10) {
			p.updateFunnelStageTimeline(journeyId, CustomerFunnel.TERMINATED_CUSTOMER);
			if (p.journeyScore < 100) {
				p.journeyScore = 100;
				p.funnelStage = FunnelMetaData.STAGE_TERMINATED_CUSTOMER;
			}
		}

		return funnelIndex;
	}

	/**
	 * @param p
	 * @return
	 */
	final static long computeTotalValueOfProfileData(AbstractProfile p) {
		long total = (long) (p.totalLeadScore + p.totalProspectScore + p.totalEngagementScore + Math.floor(p.totalCLV));
		total += (p.dataQualityScore * 10);
		total += (p.totalCreditScore * 20);
		total += (p.totalNPS + p.totalCFS + p.totalCES + +p.totalCSAT + (long) Math.abs(p.totalCAC));
		total += (p.getUpdatedAt().getTime());

		if (p.hasContactData()) {
			total += 1000;
		}

		Set<JourneyMapRefKey> mapRefKeys = p.getInJourneyMaps();
		for (JourneyMapRefKey ref : mapRefKeys) {
			if (p.hasTransactionalData(ref.getId())) {
				total += 5000;
			}
		}

		// base on type to rank value
		int baseScoreForProfileType = 10000;
		if (p.isDataVerification()) {
			baseScoreForProfileType = 1000000;
		}
		int baseOfProfileType = baseScoreForProfileType * p.type;
		total += baseOfProfileType;

		return total;
	}


	/**
	 * @param dateOfBirth
	 * @return
	 */
	static int getAgeFromDateOfBirth(Date dateOfBirth) {
		LocalDate bDate = dateOfBirth.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		LocalDate eDate = new Date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		return Period.between(bDate, eDate).getYears();
	}

	/**
	 * @param profile
	 * @param updatingAttributes
	 */
	static void updateWithAttributeMap(ProfileSingleView profile, Map<String, Object> updatingAttributes) {
		if (updatingAttributes.isEmpty()) {
			// skip
			return;
		}
		Map<String, Field> profileFields = getAutoSetDataByWebFormFields();

		updatingAttributes.forEach((fieldName, fieldValue) -> {
			updateProfileWithAttribute(profile, profileFields, fieldName, fieldValue);
		});
	}

	public static void updateProfileWithAttribute(ProfileSingleView profile, Map<String, Field> profileFields,
			String fieldName, Object fieldValue) {
		Class<?> profileClass = ProfileSingleView.class;
		Field field = profileFields.get(fieldName);
		String valueAsStr = XssFilterUtil.clean(StringUtil.safeString(fieldValue, ""));

		// logger.info("field: " + fieldName + " => " + valueAsStr);

		if (StringUtil.isNotEmpty(valueAsStr)) {
			if (field != null) {
				// enable Accessible
				field.setAccessible(true);
				try {
					AutoSetData metadata = field.getAnnotation(AutoSetData.class);

					// the name of setter method
					String methodName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
					// logger.info("methodName: " + methodName);

					if (metadata.setDataAsString()) {
						Method method = profileClass.getMethod(methodName, String.class);
						method.invoke(profile, XssFilterUtil.clean(valueAsStr));
					} else if (metadata.setDataAsInteger()) {
						Method method = profileClass.getMethod(methodName, Integer.TYPE);
						method.invoke(profile, StringUtil.safeParseInt(fieldValue, 0));
					} else if (metadata.setDataAsFloat()) {
						Method method = profileClass.getMethod(methodName, Float.TYPE);
						method.invoke(profile, StringUtil.safeParseFloat(fieldValue, 0));
					} else if (metadata.setDataAsDouble()) {
						Method method = profileClass.getMethod(methodName, Double.TYPE);
						method.invoke(profile, StringUtil.safeParseDouble(valueAsStr));
					} else if (metadata.setDataAsDate()) {
						Method method = profileClass.getMethod(methodName, Date.class);
						SimpleDateFormat dateFormat = new SimpleDateFormat(YYYY_MM_DD);
						// dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Saigon"));
						Date date = dateFormat.parse(valueAsStr);
						method.invoke(profile, date);
					} else if (metadata.setDataAsJsonMap()) {
						Method method = profileClass.getMethod(methodName, String.class);
						if (valueAsStr.startsWith("{") && valueAsStr.endsWith("}")) {
							method.invoke(profile, valueAsStr);
						}
					} else {
						System.err
								.println("Not found setter for fieldName:" + fieldName + " fieldValue: " + fieldValue);
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}

				// reset Accessible
				field.setAccessible(false);
			} else if (fieldName.startsWith(EXT)) {
				// if field is not in conventional attributes of profile, add value into
				// extAttributes
				profile.setExtAttributes(fieldName, fieldValue);
			} else {
				System.err.println("Not found setter for fieldName:" + fieldName + " fieldValue: " + fieldValue);
			}
		}
	}

	final static int computeDataQualityScore(Profile profile) {
		// final AtomicInteger dataQuality = new AtomicInteger(0);
		Map<String, AttributeMetaData> fieldConfigs = SystemConfigsManagement.getProfileMetadata();
		fieldConfigs.remove("eventStatistics");

		int finalScore = fieldConfigs.values().parallelStream().map((AttributeMetaData attr) -> {
			int dataQuality = 0;
			try {
				String fieldName = attr.getAttributeName();
				String type = attr.getType();
				String invalidWhen = attr.getInvalidWhenEqual();
				String methodName = attr.getGetterMethodName();
				Method method = profile.getClass().getMethod(methodName);
				Object value = method.invoke(profile);
				if (value != null) {
					if (type.equals(INT) || type.equals(JAVA_LANG_INTEGER)) {
						int v = Integer.parseInt(value.toString());
						int invalid = Integer.parseInt(invalidWhen.isEmpty() ? "0" : invalidWhen);
						if (v != invalid) {
							dataQuality += DataQualityScoreUtil.getBaseScoreByField(fieldName);
						}
					} else if (type.equals(DOUBLE) || type.equals(JAVA_LANG_DOUBLE)) {
						double invalid = Double.parseDouble(invalidWhen.isEmpty() ? "0" : invalidWhen);
						double v = Double.parseDouble(value.toString());
						if (v != invalid) {
							dataQuality += DataQualityScoreUtil.getBaseScoreByField(fieldName);
						}
					} else if (type.equals(BOOLEAN) || type.equals(JAVA_LANG_BOOLEAN)) {
						boolean v = Boolean.parseBoolean(value.toString());
						if (v) {
							dataQuality += DataQualityScoreUtil.getBaseScoreByField(fieldName);
						}
					} else if (type.equals(JAVA_LANG_STRING) || type.equals(JAVA_UTIL_DATE)) {
						String v = value.toString();
						if (!invalidWhen.equals(v)) {
							dataQuality += DataQualityScoreUtil.getBaseScoreByField(fieldName);
						}
					} else if (type.equals(JAVA_UTIL_MAP)) {
						Map<?, ?> v = (Map<?, ?>) value;
						if (v.size() > 0) {
							dataQuality += DataQualityScoreUtil.getBaseScoreByField(fieldName);
						}
					} else if (type.equals(JAVA_UTIL_SET)) {
						Set<?> v = (Set<?>) value;
						if (v.size() > 0) {
							dataQuality += DataQualityScoreUtil.getBaseScoreByField(fieldName);
						}
					} else {
						dataQuality += DataQualityScoreUtil.getBaseScoreByField(fieldName);
					}
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
			return dataQuality;
		}).reduce(0, Integer::sum);
		return finalScore;
	}

	/**
	 * compute the data quality score
	 * 
	 * @param profile
	 * @return
	 */
	final static int computeDataQualityScore(Profile profile, int daysSinceLastUpdate) {
		int dataQuality = computeDataQualityScore(profile);

		// eventStatistics
		Set<JourneyMapRefKey> mapRefKeys = profile.getInJourneyMaps();
		for (JourneyMapRefKey ref : mapRefKeys) {
			String journeyId = ref.getId();
			if (profile.hasActiveViewActivities(journeyId) && daysSinceLastUpdate < 10) {
				int score = DataQualityScoreUtil.getBaseScoreByField("eventStatistics") * 3;
				dataQuality += score;
			} else if (profile.hasViewActivities(journeyId) && daysSinceLastUpdate < 30) {
				dataQuality += DataQualityScoreUtil.getBaseScoreByField("eventStatistics");
			}
		}

		return dataQuality;
	}

	/**
	 * @param p
	 * @return
	 */
	public static Set<String> classifyTrafficSources(ProfileSingleView p) {
		Set<String> trafficSources = new HashSet<String>();
		Set<String> referrerChannels = p.getReferrerChannels().keySet();
		List<TouchpointHub> hubs = TouchpointHubManagement.getAll();
		Set<String> directHosts = hubs.stream().map(h -> {
			String url = h.getUrl();
			if (h.isFirstPartyData()) {
				return UrlUtil.getHostName(url);
			}
			return null;
		}).filter(s -> {
			return s != null;
		}).collect(Collectors.toSet());

		if (referrerChannels.size() == 0) {
			trafficSources.add("Direct");
		} else {
			for (String referrer : referrerChannels) {
				if (directHosts.contains(referrer)) {
					trafficSources.add("Direct");
				} else if (searchHosts.contains(referrer)) {
					trafficSources.add("Search");
				} else if (socialHosts.contains(referrer)) {
					trafficSources.add("Social");
				}
			}
		}
		return trafficSources;
	}

	public static ProfileSingleView importProfileFromCsvData(String[] headers, String[] dataRow) {
		int csvDataRowLen = dataRow.length;
		// minimum length is 9
		if (csvDataRowLen < 9) {
			System.err.println("skip due to " + csvDataRowLen);
			return null;
		}

		// 0
		String crmId = dataRow[0];
		// 1
		String creationDate = StringUtil.safeString(dataRow[1], "").trim();
		// 2
		String updateDate = StringUtil.safeString(dataRow[2], "").trim();
		// 3
		String firstName = StringUtil.safeString(dataRow[3], "").trim();
		// 4
		String lastName = StringUtil.safeString(dataRow[4], "").trim();
		// 5
		String email = StringUtil.safeString(dataRow[5], "").trim();
		// 6
		String phone = StringUtil.safeString(dataRow[6], "").trim();
		// 7
		String genderStr = StringUtil.safeString(dataRow[7], "unknown").toLowerCase();
		// 8
		int age = StringUtil.safeParseInt(dataRow[8]);
		// 9
		String dateOfBirth = StringUtil.safeString(dataRow[9], "").trim();
		// 10
		String permanentLocation = StringUtil.safeString(dataRow[10], "").trim();
		// 11
		String livingLocation = StringUtil.safeString(dataRow[11], "").trim();
		// 12
		String learningHistory = StringUtil.safeString(dataRow[12], "").trim();
		// 13
		String workingCompanies = StringUtil.safeString(dataRow[13], "").trim();
		// 14
		String jobTitles = StringUtil.safeString(dataRow[14], "").trim();
		// 15
		String businessIndustries = StringUtil.safeString(dataRow[15], "").trim();
		// 16
		String businessContacts = StringUtil.safeString(dataRow[16], "").trim();
		// 17
		String personalContacts = StringUtil.safeString(dataRow[17], "").trim();
		// 18
		String nationality = StringUtil.safeString(dataRow[18], "").trim();
		// 19
		String currentZipCode = StringUtil.safeString(dataRow[19], "").trim();
		// 20
		String locationCode = StringUtil.safeString(dataRow[20], "").trim();
		// 21
		String livingCountry = StringUtil.safeString(dataRow[21], "").trim();
		// 22
		String livingState = StringUtil.safeString(dataRow[22], "").trim();
		// 23
		String livingProvince = StringUtil.safeString(dataRow[23], "").trim();
		// 24
		String livingCity = StringUtil.safeString(dataRow[24], "").trim();
		// 25
		String livingDistrict = StringUtil.safeString(dataRow[25], "").trim();
		// 26
		String livingWard = StringUtil.safeString(dataRow[26], "").trim();
		// 27
		String mediaChannels = StringUtil.safeString(dataRow[27], "").trim();
		// 28
		String contentKeywords = StringUtil.safeString(dataRow[28], "").trim();
		// 29
		String productKeywords = StringUtil.safeString(dataRow[29], "").trim();
		// 30
		String saleAgencyLeadSource = StringUtil.safeString(dataRow[30], "").trim();
		// 31
		String salePersonSalesRepresentative = StringUtil.safeString(dataRow[31], "").trim();

		// create a default profile
		ProfileSingleView profile = ProfileSingleView.newImportedProfile(crmId, firstName, lastName, email, phone,
				genderStr, age, dateOfBirth, permanentLocation, livingLocation);

		profile.setCreatedAt(creationDate);
		profile.setUpdatedByCrmAt(updateDate);

		profile.setLearningHistory(learningHistory);
		profile.setWorkingHistory(workingCompanies);
		profile.setJobTitles(jobTitles);
		profile.setBusinessIndustries(businessIndustries);

		profile.setBusinessContacts(businessContacts);
		profile.setPersonalContacts(personalContacts);

		profile.setCurrentZipCode(currentZipCode);
		profile.setPrimaryNationality(nationality);
		profile.setLocationCode(locationCode);
		profile.setLivingCountry(livingCountry);
		profile.setLivingProvince(livingProvince);
		profile.setLivingCity(livingCity);
		profile.setLivingState(livingState);
		profile.setLivingDistrict(livingDistrict);
		profile.setLivingWard(livingWard);

		profile.setMediaChannels(mediaChannels);
		profile.setContentKeywords(contentKeywords);
		profile.setProductKeywords(productKeywords);

		profile.setSaleAgencies(saleAgencyLeadSource);
		profile.setSaleAgents(salePersonSalesRepresentative);

		if (csvDataRowLen > 32) {
			String personalProblems = dataRow[32];
			profile.setPersonalProblems(personalProblems);
		}

		if (csvDataRowLen > 33) {
			String personalInterests = dataRow[33];
			profile.setPersonalInterests(personalInterests);
		}

		if (csvDataRowLen > 34) {
			String solutionsForCustomer = dataRow[34];
			profile.setSolutionsForCustomer(solutionsForCustomer);
		}

		if (csvDataRowLen > 35) {
			String socialMediaProfilesStr = dataRow[35];
			profile.importSocialMediaProfiles(socialMediaProfilesStr);
		}

		if (csvDataRowLen > 36) {
			String primaryAvatar = dataRow[36];
			profile.setPrimaryAvatar(primaryAvatar);
		}
		
		if (csvDataRowLen > 37) {
			String dataLabelsStr = dataRow[37];
			profile.setDataLabels(dataLabelsStr);
		}

		if (csvDataRowLen > 38) {
			String username = dataRow[38];
			profile.setPrimaryUsername(username);
		}

		if (csvDataRowLen > 39) {
			String password = dataRow[39];
			profile.setPassword(password);
		}

		// custom field
		for (int i = 40; i < csvDataRowLen; i++) {
			String fieldName = headers[i];
			Object value = dataRow[i];
			profile.setExtAttributes(fieldName, value);
		}
		profile.buildHashedId();
		System.out.println(" parsed CSV for profile " + profile.getId());
		return profile;
	}
	
	
	////////////////////////////////////////////////////////////////////////////////////////////

	public static Date toISO8601WithTimezone(String dateStr, String zoneIdStr) {
		if (dateStr != null) {
			// Parse the ISO 8601 string to Instant (UTC)
			Instant instant = Instant.parse(dateStr);

			if (zoneIdStr != null) {
				// Convert Instant to ZonedDateTime in the specified timezone
				ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of(zoneIdStr));
				// Format ZonedDateTime to ISO 8601 string with timezone information
				return Date.from(zonedDateTime.toInstant());
			}
			return Date.from(instant);
		}
		return null;
	}

	public static Date toDateInGMT(String dateTimeString) {
		 // Parse the string to ZonedDateTime
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateTimeString, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        
        // Convert to Instant (in UTC/GMT)
        Instant instant = zonedDateTime.toInstant();
        
        // Convert to Date
        Date gmtDate = Date.from(instant);
		return gmtDate;
	}

	/**
	 * @param dateStr
	 * @return
	 */
	public final static Date parseDate(String dateStr) {
		return parseDate(dateStr, SystemMetaData.DEFAULT_TIME_ZONE);
	}

	/**
	 * @param dateStr in ISO 8601 "2020-03-09T02:27:41Z" or yyyy-MM-dd or dd/MM/yyyy
	 * @return A Date parsed from the string.
	 */
	public final static Date parseDate(String dateStr, String zoneIdStr) {
		if (StringUtil.isNotEmpty(dateStr)) {
			// date with time
			try {
				int indexPlus = dateStr.indexOf(StringPool.PLUS);
				if(indexPlus > 0) {
					// 2024-09-08T18:11:46+07:00
					return toDateInGMT(dateStr);
				}
				else {
					// 2024-09-08T18:20:46Z
					return toISO8601WithTimezone(dateStr, zoneIdStr);	
				}
			} catch (Exception e) {}
			
			// date with no time
			try {
				int indexMinus = dateStr.indexOf(StringPool.MINUS);
				int indexSlash = dateStr.indexOf(StringPool.SLASH);
				if (indexMinus > 3) {
					// parse yyyy-MM-dd: 2020-03-09
					DateFormat dateFormat = new SimpleDateFormat(YYYY_MM_DD);
					return dateFormat.parse(dateStr);
				}
				else if (indexSlash > 1) {
					// parse dd/MM/yyyy 09/03/2020
					DateFormat dateFormat = new SimpleDateFormat(DD_MM_YYYY);
					return dateFormat.parse(dateStr);
				}
			} catch (Exception e) {}
		}
		return null;
	}

	public static Date convertISOToGMTDate(String dateStr) {
		try {
			// Parse the ISO 8601 timestamp
			Instant instant = Instant.parse(dateStr);

			// Convert Instant to Date (Instant is UTC/GMT by default)
			return Date.from(instant);
		} catch (Exception e) {
			e.printStackTrace();
			return null; // Handle exception if necessary
		}
	}
	
	/**
	 * @param dateOfBirth
	 * @return
	 */
	public final static String formatDateInYYYY_MM_DD(Date date) {
		try {
			DateFormat dateFormat = new SimpleDateFormat(YYYY_MM_DD);
			return dateFormat.format(date);
		} catch (Exception e) {
		}
		return "";
	}

}
