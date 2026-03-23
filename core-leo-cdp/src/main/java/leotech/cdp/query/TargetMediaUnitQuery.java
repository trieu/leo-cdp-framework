package leotech.cdp.query;

import java.util.Objects;

import com.google.gson.Gson;

public class TargetMediaUnitQuery {
	String visitorId;
	int startIndex, numberResult;
	int hashCode;

	public TargetMediaUnitQuery(String visitorId, int startIndex, int numberResult) {
		super();
		this.visitorId = visitorId;		
		this.startIndex = startIndex;
		this.numberResult = numberResult;
		this.hashCode = Objects.hash(visitorId, startIndex, numberResult);
	}

	public String getVisitorId() {
		return visitorId;
	}

	public int getStartIndex() {
		return startIndex;
	}

	public int getNumberResult() {
		return numberResult;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		TargetMediaUnitQuery that = (TargetMediaUnitQuery) o;
		String myKey = visitorId + startIndex;
		String oKey = that.visitorId + that.startIndex;
		return Objects.equals(myKey, oKey);
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
}
