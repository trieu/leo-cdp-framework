package leotech.query.util;

import java.util.Comparator;

import leotech.cdp.model.asset.MeasurableItem;

public class PostRankingUtil {
	// TODO

	public static Comparator<MeasurableItem> orderByTime = new Comparator<MeasurableItem>() {

		@Override
		public int compare(MeasurableItem o1, MeasurableItem o2) {
			if (o1.getModificationTime() > o2.getModificationTime()) {
				return 1;
			} else if (o1.getModificationTime() < o2.getModificationTime()) {
				return -1;
			}
			return 0;
		}
	};
}
