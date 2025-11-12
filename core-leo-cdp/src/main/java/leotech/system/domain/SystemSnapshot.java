package leotech.system.domain;

import java.io.File;
import java.lang.management.ManagementFactory;

import com.google.gson.Gson;
import com.sun.management.OperatingSystemMXBean;

/**
 * Represents a point-in-time snapshot of the system's hardware usage.
 * Provides CPU, memory, and disk utilization in gigabytes.
 * 
 * @author Trieu Nguyen
 * @since 2024
 */
public class SystemSnapshot {

    private static final double BYTES_IN_GB = 1024.0 * 1024 * 1024;

    private final double totalDiskGB;
    private final double freeDiskGB;
    private final double usableDiskGB;
    private final double cpuLoadPercent;
    private final double totalMemoryGB;
    private final double usedMemoryGB;
    private final double freeMemoryGB;
    private final String nodeInfo; // previously hostPortBuildInfo

    public SystemSnapshot(String nodeInfo) {
        this.nodeInfo = nodeInfo;

        File root = new File("/");

        this.totalDiskGB = round(root.getTotalSpace() / BYTES_IN_GB);
        this.freeDiskGB = round(root.getFreeSpace() / BYTES_IN_GB);
        this.usableDiskGB = round(root.getUsableSpace() / BYTES_IN_GB);

        OperatingSystemMXBean osBean = (OperatingSystemMXBean)
                ManagementFactory.getOperatingSystemMXBean();

        this.cpuLoadPercent = round(osBean.getSystemCpuLoad() * 100); // system load (0–100)

        long totalMemBytes = osBean.getTotalPhysicalMemorySize();
        long freeMemBytes = osBean.getFreePhysicalMemorySize();
        long usedMemBytes = totalMemBytes - freeMemBytes;

        this.totalMemoryGB = round(totalMemBytes / BYTES_IN_GB);
        this.freeMemoryGB = round(freeMemBytes / BYTES_IN_GB);
        this.usedMemoryGB = round(usedMemBytes / BYTES_IN_GB);
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    public double getCpuLoadPercent() {
        return cpuLoadPercent;
    }

    public double getTotalMemoryGB() {
        return totalMemoryGB;
    }

    public double getUsedMemoryGB() {
        return usedMemoryGB;
    }

    public double getFreeMemoryGB() {
        return freeMemoryGB;
    }

    public double getTotalDiskGB() {
        return totalDiskGB;
    }

    public double getFreeDiskGB() {
        return freeDiskGB;
    }

    public double getUsableDiskGB() {
        return usableDiskGB;
    }

    public String getNodeInfo() {
        return nodeInfo;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
