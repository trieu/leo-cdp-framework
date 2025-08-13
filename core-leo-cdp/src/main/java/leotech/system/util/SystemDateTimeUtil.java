package leotech.system.util;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

public final class SystemDateTimeUtil {
	
	public final static boolean isPeakTime() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		if ((hour >= 18 && hour <= 23) || (hour >= 0 && hour <= 3)) {
			return true;
		}
		return false;
	}

	public final static boolean isInTigerEventLive() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		if (hour >= 18 && hour <= 22) {
			return true;
		}
		return false;
	}

	public final static boolean isMidNight() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		if (hour >= 0 && hour <= 7) {
			return true;
		}
		return false;
	}

	public final static int getDaysSinceLastUpdate(Date date) {
		LocalDate now = LocalDate.now();
		LocalDate inputDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		Period period = Period.between(inputDate, now);
		int ageOfProfileInDays = period.getDays();
		return ageOfProfileInDays;
	}

}
