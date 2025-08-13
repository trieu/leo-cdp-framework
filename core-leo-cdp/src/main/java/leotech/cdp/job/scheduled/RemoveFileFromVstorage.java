package leotech.cdp.job.scheduled;

import leotech.cdp.utils.VngCloudUtil;
import leotech.system.version.SystemMetaData;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by SangNguyen
 */
public class RemoveFileFromVstorage {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(SystemMetaData.NUMBER_CORE_CPU);
    private final VngCloudUtil vstorageUtil = new VngCloudUtil();
    private String fileName;
    private String token;
    private int minutes;

    public RemoveFileFromVstorage(String fileName, String token, int minutes) {
        this.token = token;
        this.minutes = minutes;
        this.fileName = fileName;
    }

    public RemoveFileFromVstorage() {
    }

    public VngCloudUtil getVstorageUtil() {
        return vstorageUtil;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getMinutes() {
        return minutes;
    }

    public void setMinutes(int minutes) {
        this.minutes = minutes;
    }

    public void scheduleTask() {
        Runnable task = () -> {
            if (this.token == null || this.token.isBlank()) {
                vstorageUtil.deleteFile(this.fileName);
            } else {
                vstorageUtil.deleteFile(this.fileName, this.token);
            }
        };

        scheduler.schedule(task, this.minutes, TimeUnit.MINUTES);
    }
}
