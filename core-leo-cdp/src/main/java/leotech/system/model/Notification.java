package leotech.system.model;

import java.util.Date;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import io.vertx.core.json.JsonObject;
import leotech.starter.router.NotifyUserHandler;
import leotech.system.util.IdGenerator;

/**
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public class Notification {

	static final String REGISTERED = "registered";
	
	public static final int STATUS_REMOVED = -1;
	public static final int STATUS_SENT = 0;
	public static final int STATUS_NEW = 1;

	@Expose
	String id = "";
	
	@Expose
	Date createdAt;
	
	@Expose
	String className = "";

	@Expose
	String url = "";

	@Expose
	String type = "";

	@Expose
	String message = "";
	
	@Expose
	String dataFor = "";
	
	@Expose
	int status = STATUS_NEW; 
	
	public static Notification fromJson(JsonObject j) {
		Notification n = new Notification(j);
		return n;
	}

	public Notification() {
		// gson
	}

	public Notification(JsonObject json) {
		this.className = json.getString("className", "");
		this.type = json.getString("type", "");
		this.url = json.getString("url", "");
		this.message = json.getString("message", "");
		this.dataFor = json.getString("dataFor", "");
		this.buildId();
	}
	
	public Notification(String className, String type, String message, String url, String dataFor) {
		super();
		this.className = className;
		this.type = type;
		this.message = message;
		this.url = url;
		this.dataFor = dataFor;
		this.buildId();
	}
	
	public boolean notifyPercentage() {
		return type.contains("percentage");
	}
	
	
	public static Notification registeredOk(String message) {
		Notification n = new Notification();
		n.className = NotifyUserHandler.class.getSimpleName();
		n.type = REGISTERED;
		n.message = message;
		n.buildId();
		return n;
	}
	
	private void buildId() {
		this.id = IdGenerator.createHashedId(this.className + this.message + this.status + this.type + this.url + this.dataFor);
		this.status = STATUS_NEW;
		this.createdAt = new Date();
	}
	
	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getDataFor() {
		return dataFor;
	}

	public void setDataFor(String dataFor) {
		this.dataFor = dataFor;
	}

	public String toJson() {
		return new Gson().toJson(this);
	}
	
	public JsonObject toJsonObject() {
		return new JsonObject(this.toJson());
	}
	
	@Override
	public String toString() {
		return toJson();
	}

	public boolean isNew() {
		return this.status == STATUS_NEW;
	}

}
