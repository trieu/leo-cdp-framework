package leotech.cdp.domain.scoring;

/**
 * Retention Period Type: <br>
 * DAY(1) <br>
 * WEEK(2) <br>
 * MONTH(3) <br>
 * YEAR(4)
 * 
 * @author tantrieuf31
 * @since 2023
 *
 */
public enum RetentionPeriodType {
	DAY(1), WEEK(2), MONTH(3), YEAR(4);

	private final int value;

	private RetentionPeriodType(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}
}
