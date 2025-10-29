package leotech.starter;

import leotech.system.HttpWorker;
import leotech.system.util.LogUtil;
import leotech.system.version.SystemMetaData;

public class UploadFileHttpStarter {
	
	private static final String UPLOAD_FILE_WORKER = "uploadFileWorker";

	public static void main(String[] args) throws Exception {
		LogUtil.loadLoggerConfigs();
		SystemMetaData.initTimeZoneGMT();
		HttpWorker.start(UPLOAD_FILE_WORKER);
	}
	
}
