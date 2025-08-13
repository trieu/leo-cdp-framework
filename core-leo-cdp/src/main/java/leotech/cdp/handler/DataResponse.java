package leotech.cdp.handler;

public class DataResponse {

	protected int status = 0;
	protected String message = "";

	public DataResponse() {
		super();
	}
	

	public DataResponse(int status, String message) {
		super();
		this.status = status;
		this.message = message;
	}


	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	

}