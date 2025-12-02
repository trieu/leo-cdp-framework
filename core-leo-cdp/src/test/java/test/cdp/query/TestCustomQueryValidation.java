package test.cdp.query;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import leotech.cdp.query.SegmentQuery;

/**
 * all unit-test cases for SegmentQuery.customQueryValidation
 */
public class TestCustomQueryValidation {

	// ============================
	// WRITE KEYWORD SHOULD FAIL
	// ============================

	@Test
	void testQueryWithUpdateShouldFail() {
		String aql = "FOR p IN system_event\n" + "    FILTER p.action == \"brevo\"\n"
				+ "    UPDATE p WITH { action: \"\" } IN system_event";

		assertFalse(SegmentQuery.customQueryValidation(aql), "Query containing UPDATE must be rejected");
	}

	@Test
	void testQueryWithInsertShouldFail() {
		String aql = "INSERT { name: \"x\" } INTO system_event";

		assertFalse(SegmentQuery.customQueryValidation(aql), "Query containing INSERT must be rejected");
	}

	@Test
	void testQueryWithRemoveShouldFail() {
		String aql = "FOR x IN system_event FILTER x.status==1 REMOVE x IN system_event";

		assertFalse(SegmentQuery.customQueryValidation(aql), "Query containing REMOVE must be rejected");
	}

	@Test
	void testQueryWithUpsertShouldFail() {
		String aql = "UPSERT { k:1 } INSERT { k:1 } UPDATE { k:2 } IN system_event";

		assertFalse(SegmentQuery.customQueryValidation(aql), "Query containing UPSERT must be rejected");
	}

	// ============================
	// CDP_PROFILE SHOULD FAIL
	// ============================

	@Test
	void testUsingForbiddenCollectionShouldFail() {
		String aql = "FOR d IN cdp_profile FILTER d.status > 0 RETURN d";

		assertFalse(SegmentQuery.customQueryValidation(aql), "Direct access to cdp_profile must be rejected");
	}

	// ============================
	// NO FILTER SHOULD FAIL
	// ============================

	@Test
	void testMissingFilterShouldFail() {
		String aql = "FOR x IN system_event RETURN x";

		assertFalse(SegmentQuery.customQueryValidation(aql), "Query without FILTER must be rejected");
	}

	// ============================
	// SAFE CASES SHOULD PASS
	// ============================

	@Test
	void testQueryWithUpdateShouldOK() {
		String aql = "LET nowISO = DATE_ISO8601(DATE_NOW())\n"
				+ "LET sevenDaysAgoISO = DATE_ISO8601(DATE_SUBTRACT(DATE_NOW(), 7, \"days\"))\n"
				+ "LET events = [\"remove-trendline\"]\n" + "LET useCount = LENGTH(\n"
				+ "   FOR e IN cdp_trackingevent\n" + "       FILTER e.refProfileId == d._key\n"
				+ "       AND e.metricName IN events\n" + "       AND e.createdAt >= sevenDaysAgoISO\n"
				+ "       AND e.createdAt <= nowISO\n" + "       RETURN 1\n" + ")\n" + "FILTER useCount > 30";

		assertTrue(SegmentQuery.customQueryValidation(aql), "Query is valid and should be accepted");
	}

	@Test
	void testStringContainsRemoveButIsSafe() {
		String aql = "FOR x IN events\n" + "    FILTER x.tag == \"remove-trendline\"\n" + "    RETURN x";

		assertTrue(SegmentQuery.customQueryValidation(aql), "Keyword inside string literal must NOT cause rejection");
	}

	@Test
	void testVariableNameContainsRemoveShouldPass() {
		String aql = "LET removeSomething = 1\n" + "FOR x IN items\n" + "    FILTER x.v > removeSomething\n"
				+ "    RETURN x";

		assertTrue(SegmentQuery.customQueryValidation(aql), "Variable names containing 'remove' should be allowed");
	}

	@Test
	void testQueryMultiLineSafeShouldPass() {
		String aql = "FOR u IN users\n" + "    FILTER u.age > 18\n" + "    FILTER u.status == \"active\"\n"
				+ "RETURN u";

		assertTrue(SegmentQuery.customQueryValidation(aql), "Normal read-only filtered query must be accepted");
	}
}
