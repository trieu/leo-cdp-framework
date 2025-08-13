package leotech.cdp.dao;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;

import leotech.cdp.model.customer.Device;
import leotech.system.config.AqlTemplate;
import leotech.system.util.database.ArangoDbCommand;
import leotech.system.util.database.ArangoDbUtil;

/**
 * @author tantrieuf31
 * @since 2020
 *
 */
public final class DeviceDaoUtil extends AbstractCdpDatabaseUtil {

	private static final Device UNKNOWN_DEVICE = new Device("Unknown Device");
	static final int TIME_TO_PROCESS = 2000;// milisecs
	private static Queue<Device> localQueue = new ConcurrentLinkedQueue<>();
	private static Timer timer = new Timer(true);

	static {
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				flushData();
			}
		}, TIME_TO_PROCESS, TIME_TO_PROCESS);
	}

	static final String AQL_GET_DEVICE_BY_ID = AqlTemplate.get("AQL_GET_DEVICE_BY_ID");

	/**
	 * @param d
	 * @return
	 */
	public static String save(Device d) {
		if(d != null) {
			if (d.dataValidation()) {
				localQueue.add(d);
				return d.getId();
			}
		}
		return null;
	}

	/**
	 * 
	 */
	public static void flushData() {
		while (!localQueue.isEmpty()) {
			commitToDatabase(localQueue.poll());
		}
	}

	/**
	 * @param d
	 */
	static void commitToDatabase(Device d) {
		if (d != null) {
			ArangoCollection col = d.getDbCollection();
			String id = d.getId();
			if (col != null) {
				ArangoDatabase db = getCdpDatabase();
				boolean isExisted = ArangoDbUtil.isExistedDocument(db, Device.COLLECTION_NAME, id);
				if (!isExisted) {
					col.insertDocument(d);
				} else {
					col.updateDocument(id, d, getUpdateOptions());
				}
			}
		}
	}

	/**
	 * @param id
	 * @return
	 */
	public static Device getById(String id) {
		ArangoDatabase db = getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("id", id);
		Device p = new ArangoDbCommand<Device>(db, AQL_GET_DEVICE_BY_ID, bindVars, Device.class).getSingleResult();
		if (p == null) {
			p = UNKNOWN_DEVICE;
		}
		return p;
	}

}
