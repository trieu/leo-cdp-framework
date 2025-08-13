package leotech.system.model;

import com.google.gson.Gson;

public final class ImportingResult {
	public final int importedOk;
	public final int importedFail;
	
	public ImportingResult(int importedOk, int importedFail) {
		super();
		this.importedOk = importedOk;
		this.importedFail = importedFail;
	}
	
	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
}
