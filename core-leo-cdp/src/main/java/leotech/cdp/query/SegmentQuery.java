package leotech.cdp.query;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.itfsw.query.builder.ArangoDbBuilderFactory;
import com.itfsw.query.builder.support.builder.ArangoDbBuilder;
import com.itfsw.query.builder.support.model.result.ArangoDbQueryResult;

import leotech.cdp.model.RefKey;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.customer.ProfileModelUtil;
import leotech.system.exception.InvalidDataException;
import leotech.system.util.IdGenerator;
import rfx.core.util.StringUtil;

/**
 * ArangoDB query builder for segment model
 * 
 * @author tantrieuf31
 * @since 2020
 *
 */
public final class SegmentQuery {

	private static final int MINIMUM_QUERY_LENGTH = 5;
	private static final String JSON_QUERY_RULES_ERROR_MESSAGE = "jsonQueryRules must not be NULL";
	private static final String FALSE = " FALSE ";
	String queryHashedId = "";
	String segmentId = "";
	String segmentName = "";
	int indexScore = 0;

	boolean pagination = true;
	boolean countingTotal = false;
	boolean realtimeQuery = true;
	boolean withLastEvents = false;

	String jsonQueryRules = "{}";
	String filterDataSet = "";
	String parsedFilterAql = "";
	String finalCountingAql = "";

	int startIndex = 0;
	int numberResult = 20;
	List<String> selectedFields;

	// BEGIN constructors

	public SegmentQuery() {
		// gson
	}

	public SegmentQuery(int indexScore, String segmentId, String segmentName, String jsonQueryRules,
			String customQueryFilter, List<String> selectedFields, boolean realtimeQuery) {
		super();
		if (jsonQueryRules != null) {
			this.indexScore = indexScore;
			this.segmentId = segmentId;
			this.segmentName = segmentName;
			this.selectedFields = selectedFields;
			this.realtimeQuery = realtimeQuery;
			parseQueryRulesAndCustomQuery(jsonQueryRules, customQueryFilter);
		} else {
			throw new IllegalArgumentException(JSON_QUERY_RULES_ERROR_MESSAGE);
		}
	}

	public SegmentQuery(int indexScore, String segmentId, String segmentName, String jsonQueryRules,
			String customQueryFilter, List<String> selectedFields, boolean realtimeQuery, boolean withLastEvents) {
		super();
		if (jsonQueryRules != null) {
			this.indexScore = indexScore;
			this.segmentId = segmentId;
			this.segmentName = segmentName;
			this.selectedFields = selectedFields;
			this.realtimeQuery = realtimeQuery;
			this.withLastEvents = withLastEvents;
			parseQueryRulesAndCustomQuery(jsonQueryRules, customQueryFilter);
		} else {
			throw new IllegalArgumentException(JSON_QUERY_RULES_ERROR_MESSAGE);
		}
	}

	public SegmentQuery(String beginFilterDate, String endFilterDate, String jsonQueryRules, String customQueryFilter,
			List<String> selectedFields) {
		super();
		if (jsonQueryRules != null) {
			parseQueryRulesAndCustomQuery(jsonQueryRules, customQueryFilter);
			this.selectedFields = selectedFields;
			this.segmentName = "";
		} else {
			throw new IllegalArgumentException(JSON_QUERY_RULES_ERROR_MESSAGE);
		}
	}

	public SegmentQuery(boolean pagination, boolean countingTotal, String jsonQueryRules, String customQueryFilter,
			int startIndex, int numberResult, List<String> selectedFields) {
		super();
		if (jsonQueryRules != null) {
			this.pagination = pagination;
			this.countingTotal = countingTotal;

			this.startIndex = startIndex;
			this.numberResult = numberResult;
			this.selectedFields = selectedFields;
			parseQueryRulesAndCustomQuery(jsonQueryRules, customQueryFilter);
			this.segmentName = "";
		} else {
			throw new IllegalArgumentException(JSON_QUERY_RULES_ERROR_MESSAGE);
		}
	}

	// END constructors

	public boolean isPagination() {
		return pagination;
	}

	public void setPagination(boolean pagination) {
		this.pagination = pagination;
	}

	public boolean isCountingTotal() {
		return countingTotal;
	}

	public void setCountingTotal(boolean countingTotal) {
		this.countingTotal = countingTotal;
	}

	public String getJsonQueryRules() {
		return jsonQueryRules;
	}

	/**
	 * @param jsonQueryRules
	 * @param customQueryFilter
	 */
	public void parseQueryRulesAndCustomQuery(String jsonQueryRules, String customQueryFilter) {
		System.out.println(" => parseQueryRulesAndCustomQuery of segment: " + segmentName);
		// System.out.println(" => parseQueryRules \n " + jsonQueryRules);

		try {
			// must is a valid JSON string
			if (jsonQueryRules.length() >= MINIMUM_QUERY_LENGTH) {
				this.jsonQueryRules = jsonQueryRules;
				ArangoDbBuilder builder = new ArangoDbBuilderFactory().builder();
				ArangoDbQueryResult result = builder.build(jsonQueryRules);

				this.filterDataSet = result.getFilterDataSet();
				this.parsedFilterAql = result.getQuery().trim();
				if (this.parsedFilterAql.equals("( )")) {
					this.parsedFilterAql = FALSE;
				}
			}

			// add Custom Query Filter into parsedFilterAql
			if (StringUtil.isNotEmpty(customQueryFilter)) {
				customQueryFilter = customQueryFilter.trim();

				// make sure its do not contact cdp_profile
				if (customQueryValidation(customQueryFilter)) {
					if (this.parsedFilterAql.equals(FALSE)) {
						this.parsedFilterAql = customQueryFilter;
					} else {
						this.parsedFilterAql += (" " + customQueryFilter);
					}
				} else {
					throw new InvalidDataException("validateCustomQueryFilter FAILED for value: " + customQueryFilter);
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		// avoid NullPointerException
		if (this.parsedFilterAql == null) {
			this.parsedFilterAql = "";
		}
	}

	/**
	 * Function to validate if the input only contains FILTER conditions and not
	 * contains cdp_profile
	 * 
	 * @param customQueryFilter
	 * @return boolean
	 */
	public static boolean customQueryValidation(String customQueryFilter) {
		if (customQueryFilter == null || customQueryFilter.isEmpty()) {
			return false;
		}

		// Normalize casing
		String lower = customQueryFilter.toLowerCase();

		// 1. Strip out all string literals (both "..." and '...')
		// This prevents false positives like "remove-trendline"
		String noStrings = lower.replaceAll("\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'", "");

		// 2. Remove ALL newline characters to avoid regex boundary issues
		String sanitized = noStrings.replace("\n", "").replace("\r", "");

		// 3. Block forbidden collection reference
		if (sanitized.contains(Profile.COLLECTION_NAME.toLowerCase())) {
			return false;
		}

		// 4. Block AQL write keywords using proper regex
		String[] dangerousTokens = { "update", "remove", "insert", "replace", "upsert" };

		for (String kw : dangerousTokens) {
			Pattern p = Pattern.compile("\\b" + kw + "\\b");
			if (p.matcher(sanitized).find()) {
				return false;
			}
		}

		// 5. Must contain at least one FILTER
		if (!sanitized.contains("filter")) {
			return false;
		}

		return true;
	}

	public boolean isQueryReadyToRun() {
		return StringUtil.isNotEmpty(this.parsedFilterAql);
	}

	public String getParsedFilterAql() {
		return parsedFilterAql;
	}

	protected void setParsedFilterAql(String parsedFilterAql) {
		this.parsedFilterAql = parsedFilterAql;
	}

	public String getFilterDataSet() {
		return filterDataSet;
	}

	public int getStartIndex() {
		return startIndex;
	}

	public void setStartIndex(int startIndex) {
		if (startIndex >= 0) {
			this.startIndex = startIndex;
		}
	}

	public int getNumberResult() {
		return numberResult;
	}

	public void setNumberResult(int numberResult) {
		if (numberResult > 0) {
			this.numberResult = numberResult;
		}
	}

	public List<String> getSelectedFields() {
		return selectedFields;
	}

	public void setSelectedFields(List<String> selectedFields) {
		this.selectedFields = selectedFields;
	}

	public String getSegmentId() {
		return segmentId;
	}

	public void setSegmentId(String segmentId) {
		this.segmentId = segmentId;
	}

	public String getSegmentName() {
		return segmentName;
	}

	public void setSegmentName(String segmentName) {
		this.segmentName = segmentName;
	}

	public int getIndexScore() {
		return indexScore;
	}

	public void setIndexScore(int indexScore) {
		this.indexScore = indexScore;
	}

	public String getQueryHashedId() {
		return queryHashedId;
	}

	public boolean isRealtimeQuery() {
		return realtimeQuery;
	}

	public void setRealtimeQuery(boolean realtimeQuery) {
		this.realtimeQuery = realtimeQuery;
	}

	public boolean isWithLastEvents() {
		return withLastEvents;
	}

	public void setWithLastEvents(boolean withLastEvents) {
		this.withLastEvents = withLastEvents;
	}

	public RefKey buildRefKey() {
		if (StringUtil.isNotEmpty(this.segmentId) && StringUtil.isNotEmpty(this.segmentName)) {
			return new RefKey(this.segmentId, this.segmentName, this.indexScore, queryHashedId);
		}
		return null;
	}

	// ----- BEGIN query builder

	public String getQueryWithFiltersAndPagination() {
		if (selectedFields.size() == 0) {
			selectedFields = ProfileModelUtil.getExposedFieldNamesInSegmentList();
		}
		return ProfileQueryBuilder.buildArangoQueryString(withLastEvents, filterDataSet, parsedFilterAql, startIndex,
				numberResult, selectedFields);
	}

	public String getQueryForAllProfileIdsInSegment() {
		return ProfileQueryBuilder.buildArangoQueryString(false, filterDataSet, parsedFilterAql, startIndex,
				numberResult, selectedFields);
	}

	public String getCountingQueryWithDateTimeFilter() {
		this.finalCountingAql = ProfileQueryBuilder.buildCoutingQuery(null, filterDataSet, parsedFilterAql);
		return this.finalCountingAql;
	}

	public String getQueryToCheckProfile(String profileId) {
		return ProfileQueryBuilder.buildCoutingQuery(profileId, filterDataSet, parsedFilterAql);
	}

	public String getQueryToBuildSegmentIndex() {
		return ProfileQueryBuilder.buildSegmentQueryToBuildIndex(this.segmentId, filterDataSet, parsedFilterAql);
	}

	public String getQueryTotal() {
		return ProfileQueryBuilder.buildCoutingQuery(parsedFilterAql);
	}

	public String updateStartIndexAndGetDataQuery(int startIndex) {
		this.startIndex = startIndex;
		return getQueryWithFiltersAndPagination();
	}

	// ----- END query builder

	public String buildQueryHashedId() {
		if (StringUtil.isEmpty(this.queryHashedId)) {
			String keyHint = this.realtimeQuery + this.segmentId + this.segmentName + this.jsonQueryRules
					+ this.startIndex + this.numberResult;
			this.queryHashedId = IdGenerator.createHashedId(keyHint);
		}
		return this.queryHashedId;
	}

	@Override
	public int hashCode() {
		this.buildQueryHashedId();
		if (this.queryHashedId != null) {
			return Objects.hash(this.queryHashedId);
		}
		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		return this.toString().equals(obj.toString());
	}

	@Override
	public String toString() {
		return buildQueryHashedId();
	}

}
