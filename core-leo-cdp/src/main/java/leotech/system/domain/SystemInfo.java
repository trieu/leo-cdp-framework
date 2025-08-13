package leotech.system.domain;

import java.io.File;
import java.lang.management.ManagementFactory;

import com.google.gson.Gson;
import com.sun.management.OperatingSystemMXBean;

/**
 * to get System Info
 * 
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public class SystemInfo {

	private static final double IN_GB = 1024.0 * 1024 * 1024;
	private double totalDiskSpace;
	private double freeDiskSpace;
	private double usableDiskSpace;
	private double cpuLoad;
	private double totalMemory;
	private double usedMemory;
	private double freeMemory;

	// Constructor to initialize the system info
	public SystemInfo() {
		File file = new File("/"); // Replace "/" with the desired directory path

		// Disk space information
		this.totalDiskSpace =   round(file.getTotalSpace() / IN_GB); // GB
		this.freeDiskSpace = round(file.getFreeSpace() / IN_GB); // GB
		this.usableDiskSpace = round(file.getUsableSpace() / IN_GB); // GB

		// Memory and CPU information
		OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
		this.cpuLoad = osBean.getSystemLoadAverage() * 100; // CPU load percentage

		// Memory information in GB
		long totalMemorySize = osBean.getTotalPhysicalMemorySize();
		long freeMemorySize = osBean.getFreePhysicalMemorySize();
		this.totalMemory = round(totalMemorySize / IN_GB); // GB
		this.freeMemory = round(freeMemorySize / IN_GB); // GB
		
		this.usedMemory = round(totalMemorySize - freeMemorySize) / IN_GB; // GB
	}
	
	static final double round(double d) {
		return Math.round(d * 1000.0) / 1000.0;
	}

	public void setUsableDiskSpace(long usableDiskSpace) {
		this.usableDiskSpace = usableDiskSpace;
	}

	public double getCpuLoad() {
		return cpuLoad;
	}

	public void setCpuLoad(double cpuLoad) {
		this.cpuLoad = cpuLoad;
	}

	public double getTotalMemory() {
		return totalMemory;
	}

	public void setTotalMemory(double totalMemory) {
		this.totalMemory = totalMemory;
	}

	public double getUsedMemory() {
		return usedMemory;
	}

	public void setUsedMemory(double usedMemory) {
		this.usedMemory = usedMemory;
	}

	public double getFreeMemory() {
		return freeMemory;
	}

	public void setFreeMemory(double freeMemory) {
		this.freeMemory = freeMemory;
	}

	public double getTotalDiskSpace() {
		return totalDiskSpace;
	}

	public void setTotalDiskSpace(double totalDiskSpace) {
		this.totalDiskSpace = totalDiskSpace;
	}

	public double getFreeDiskSpace() {
		return freeDiskSpace;
	}

	public void setFreeDiskSpace(double freeDiskSpace) {
		this.freeDiskSpace = freeDiskSpace;
	}

	public double getUsableDiskSpace() {
		return usableDiskSpace;
	}

	public void setUsableDiskSpace(double usableDiskSpace) {
		this.usableDiskSpace = usableDiskSpace;
	}
	
	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

}
