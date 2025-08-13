package leotech.system.model;

public final class ScheduledJobResult {

	boolean ok = false;
	long startTime = 0, endTime = 0;

	public ScheduledJobResult() {
		startTime = System.currentTimeMillis();
	}

	public long doneJobAndReturnElapsedTime(boolean ok) {
		this.ok = ok;
		this.endTime = System.currentTimeMillis();
		long d = endTime - startTime;
		return d;
	}
	
	public boolean isOk() {
		return ok;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getEndTime() {
		return endTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}
}
