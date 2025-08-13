package leotech.cdp.model;

import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

/**
 * reference key for object on CDP model
 * 
 * @author tantrieuf31
 * @since 2021
 *
 */
public class RefKey implements Comparable<RefKey> {

	@Expose
	protected String id;
	
	@Expose
	protected String name = "";
	
	@Expose
	protected String type = "";
	
	protected int indexScore = 0;
	
	@Expose
	protected int lastDataSynch = 0;
	
	protected String queryHashedId = "";

	public RefKey() {
		// default
	}
	
	public RefKey(String id) {
		super();
		this.id = id;
	}

	public RefKey(String id, String name, String type) {
		super();
		this.id = id;
		this.name = name;
		this.type = type;
	}
	
	public RefKey(String id, String name, String type, int indexScore) {
		super();
		this.id = id;
		this.name = name;
		this.type = type;
		this.indexScore = indexScore;
	}

	public RefKey(String id, String name, int indexScore, String queryHashedId) {
		super();
		this.id = id;
		this.name = name;
		this.indexScore = indexScore;
		this.queryHashedId = queryHashedId;
	}
	
	public RefKey(String id, String name, String type, int indexScore, String queryHashedId) {
		super();
		this.id = id;
		this.name = name;
		this.type = type;
		this.indexScore = indexScore;
		this.queryHashedId = queryHashedId;
	}


	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getIndexScore() {
		return indexScore;
	}

	public void setIndexScore(int indexScore) {
		this.indexScore = indexScore;
	}

	public int getLastDataSynch() {
		return lastDataSynch;
	}

	public void setLastDataSynch(int lastDataSynch) {
		this.lastDataSynch = lastDataSynch;
	}

	public String getQueryHashedId() {
		return queryHashedId;
	}

	public void setQueryHashedId(String queryHashedId) {
		this.queryHashedId = queryHashedId;
	}
	
	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj) {
		return this.hashCode() == obj.hashCode();
	}

	@Override
	public int compareTo(RefKey o) {
		if (this.indexScore > o.getIndexScore()) {
			return 1;
		} else if (this.indexScore < o.getIndexScore()) {
			return -1;
		}
		return 0;
	}
}
