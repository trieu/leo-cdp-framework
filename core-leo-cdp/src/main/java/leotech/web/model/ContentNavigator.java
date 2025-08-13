package leotech.web.model;

import com.google.gson.Gson;

public abstract class ContentNavigator implements Comparable<ContentNavigator> {

	protected final String id;
	protected final String uri;
	protected final String name;
	protected final long rankScore;

	public ContentNavigator(String id, String uri, String name, long rankScore) {
		super();
		this.id = id;
		this.uri = uri;
		this.name = name;
		this.rankScore = rankScore;
	}

	public String getId() {
		return id;
	}

	public String getUri() {
		return uri;
	}

	public String getName() {
		return name;
	}

	public long getRankScore() {
		return rankScore;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

	@Override
	public int compareTo(ContentNavigator o) {
		if (this.rankScore < o.getRankScore()) {
			return 1;
		} else if (this.rankScore > o.getRankScore()) {
			return -1;
		}
		return 0;
	}

}
