package test.util;

import java.time.Instant;
import java.util.Date;

import org.joda.time.DateTime;

import rfx.core.util.DateTimeUtil;

public class TestDateTimeParser {

	public static void main(String[] args) {
		String eventTime = "2022-10-12T08:54:25.110Z";
		DateTime dt = new DateTime(eventTime);
		System.out.println(DateTimeUtil.formatDateHourMinute(dt.toDate()));

		Date createdAt = Date.from(Instant.parse(eventTime));
		System.out.println(DateTimeUtil.formatDateHourMinute(createdAt));
	}
}
