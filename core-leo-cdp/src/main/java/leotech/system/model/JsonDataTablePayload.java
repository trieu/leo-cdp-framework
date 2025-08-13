package leotech.system.model;

import com.google.gson.annotations.Expose;

public class JsonDataTablePayload extends JsonDataPayload {
	
	public static final JsonDataTablePayload data(String uri, Object data, long recordsTotal, long recordsFiltered, int draw) {
		JsonDataTablePayload model = new JsonDataTablePayload(uri, data);
		model.setRecordsTotal(recordsTotal);
		model.setRecordsFiltered(recordsFiltered);
		model.setDraw(draw);
		return model;
	}
	
	public static final JsonDataTablePayload data(String uri, Object data, long recordsTotal) {
		return data(uri, data, recordsTotal, recordsTotal, 1);
	}
	
	protected JsonDataTablePayload(String uri, Object data) {
		super(uri, data, false);
	}

	@Expose
	long recordsTotal = 0;
	
	@Expose
	long recordsFiltered = 0;
	
	@Expose
	int draw = 2;
	
	public long getRecordsTotal() {
		return recordsTotal;
	}

	public void setRecordsTotal(long recordsTotal) {
		this.recordsTotal = recordsTotal;
	}

	public long getRecordsFiltered() {
		return recordsFiltered;
	}

	public void setRecordsFiltered(long recordsFiltered) {
		this.recordsFiltered = recordsFiltered;
	}

	public int getDraw() {
		return draw;
	}

	public void setDraw(int draw) {
		this.draw = draw;
	}
}
